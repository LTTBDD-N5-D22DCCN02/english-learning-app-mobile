use estudy;
drop database estudy;

select * from users;
select * from flashcard_sets;
select * from flashcards;
select * from comments;
select * from invalidated_token;
select * from quiz_session;
select * from study_records;

USE estudy;
SELECT f.term, f.deleted, fs.name
FROM flashcards f
JOIN flashcard_sets fs ON fs.id = f.flashcardset_id
ORDER BY fs.name, f.term;


-- Kiểm tra index
SHOW INDEX FROM flashcards WHERE Column_name = 'term';
-- Nếu có → xóa
ALTER TABLE flashcards DROP INDEX term;
-- Reset data
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE study_records;
TRUNCATE TABLE quiz_session;
TRUNCATE TABLE flashcards;
TRUNCATE TABLE flashcard_sets;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;