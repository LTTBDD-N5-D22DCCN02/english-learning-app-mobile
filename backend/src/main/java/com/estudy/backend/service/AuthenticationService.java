package com.estudy.backend.service;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import com.estudy.backend.dto.request.*;
import com.estudy.backend.dto.response.AuthenticationResponse;
import com.estudy.backend.dto.response.IntrospectResponse;
import com.estudy.backend.dto.response.UserResponse;
import com.estudy.backend.entity.InvalidatedToken;
import com.estudy.backend.entity.User;
import com.estudy.backend.exception.AppException;
import com.estudy.backend.exception.ErrorCode;
import com.estudy.backend.mapper.UserMapper;
import com.estudy.backend.repository.InvalidatedTokenRepository;
import com.estudy.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {
    UserRepository userRepository;
    InvalidatedTokenRepository invalidatedTokenRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;

    @NonFinal
    @Value("${jwt.signerKey}")
    protected String signerKey;

    @NonFinal
    @Value("${jwt.valid-duration}")
    protected long validDuration;

    @NonFinal
    @Value("${jwt.refreshable-duration}")
    protected long refreshableDuration;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsernameAndDeletedFalse(request.getUsername()))
            throw new AppException(ErrorCode.USER_EXISTED);

        if (userRepository.existsByEmailAndDeletedFalse(request.getEmail()))
            throw new AppException(ErrorCode.USER_EXISTED);

        if (userRepository.existsByPhoneAndDeletedFalse(request.getPhone()))
            throw new AppException(ErrorCode.USER_EXISTED);

        User user = userMapper.toUser(request);

        user.setPassword(passwordEncoder.encode(request.getPassword()));

        userRepository.save(user);

        return userMapper.toUserResponse(user);
    }

    @Transactional
    public IntrospectResponse introspect(IntrospectRequest request) throws JOSEException, ParseException {
        var token = request.getToken();

        boolean isValid = true;

        try {
            verifyToken(token, false);
        } catch (AppException e) {
            isValid = false;
        }

        return IntrospectResponse.builder().valid(isValid).build();
    }

    @Transactional
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        User user = userRepository.findByUsernameAndDeletedFalse(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());

        if (!authenticated) throw new AppException(ErrorCode.UNAUTHENTICATED);

        var token = generateToken(user);

        return AuthenticationResponse.builder().token(token).authenticated(true).build();
    }

    @Transactional
    public void logout(LogoutRequest request) throws ParseException, JOSEException {
        try {
            SignedJWT signToken = verifyToken(request.getToken(), true);
            String jit = signToken.getJWTClaimsSet().getJWTID();
            Date expiryTime = signToken.getJWTClaimsSet().getExpirationTime();

            InvalidatedToken invalidatedToken =
                    InvalidatedToken.builder().id(jit).expiryTime(expiryTime).build();

            invalidatedTokenRepository.save(invalidatedToken);
        } catch (AppException e) {
            log.info("Token already expired");
        }
    }

    @Transactional
    public AuthenticationResponse refreshToken(RefreshRequest request) throws ParseException, JOSEException {
        var signedJWT = verifyToken(request.getToken(), true);

        invalidatedTokenRepository.save(InvalidatedToken.builder()
                .id(signedJWT.getJWTClaimsSet().getJWTID())
                .expiryTime(signedJWT.getJWTClaimsSet().getExpirationTime())
                .build());

        var username = signedJWT.getJWTClaimsSet().getSubject();
        var user = userRepository.findByUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        var token = generateToken(user);

        return AuthenticationResponse.builder().token(token).authenticated(true).build();
    }
        // Helper: Lấy thông tin user đang đăng nhập từ JWT
        public UserResponse getCurrentUser() {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();

            User user = userRepository.findByUsernameAndDeletedFalse(username)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

            return userMapper.toUserResponse(user);
        }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private SignedJWT verifyToken(String token, boolean isRefresh) throws JOSEException, ParseException {
        JWSVerifier verifier = new MACVerifier(signerKey.getBytes());

        SignedJWT signedJWT = SignedJWT.parse(token);

        Date expiryTime = (isRefresh)
                ? new Date(signedJWT
                .getJWTClaimsSet()
                .getIssueTime()
                .toInstant()
                .plus(refreshableDuration, ChronoUnit.SECONDS)
                .toEpochMilli())
                : signedJWT.getJWTClaimsSet().getExpirationTime();

        boolean verified = signedJWT.verify(verifier);

        if (!(verified && expiryTime.after(new Date()))) throw new AppException(ErrorCode.UNAUTHENTICATED);

        if (invalidatedTokenRepository.existsById(signedJWT.getJWTClaimsSet().getJWTID()))
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        return signedJWT;
    }

    private String generateToken(User user) {
        // header - Contains the algorithm used
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        // payload - Contains the data included in the token
        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .issuer("estudy.com")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now()
                                .plus(validDuration, ChronoUnit.SECONDS)
                                .toEpochMilli()
                ))
                .jwtID(UUID.randomUUID().toString())
                .claim("userId", user.getId().toString())
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());

        // Create and signature token
        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(signerKey.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create token", e);
            throw new JwtException("Cannot create JWT token", e);
        }
    }
}
