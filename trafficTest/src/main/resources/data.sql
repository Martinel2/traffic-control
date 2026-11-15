-- 테스트용 사용자 1만 명. 서버가 뜰 때마다 테이블을 새로 만들고(ddl-auto=create) 이 데이터를 넣는다.
INSERT INTO user_data (id, name) SELECT X, CONCAT('user', X) FROM SYSTEM_RANGE(1, 10000);
