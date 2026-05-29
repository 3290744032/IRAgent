-- IRAgent 数据库初始化脚本
-- 数据库: PostgreSQL
-- 初始化: 2026-05-15

-- ==========================================
-- 第一部分: 用户认证模块 (v1)
-- ==========================================

-- 创建用户表
CREATE TABLE IF NOT EXISTS users (
    user_id SERIAL PRIMARY KEY,
    account VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    telphone VARCHAR(20) NOT NULL UNIQUE,
    nickname VARCHAR(255),
    age INTEGER,
    gender VARCHAR(10),
    avatar VARCHAR(255),
    status INTEGER DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_user_account ON users(account);
CREATE INDEX IF NOT EXISTS idx_user_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_user_telphone ON users(telphone);

-- 创建会话表
CREATE TABLE IF NOT EXISTS conversation (
    conversation_id VARCHAR(36) PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(user_id),
    name VARCHAR(200),
    description VARCHAR(500),
    status VARCHAR(20) DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_conversation_user_id ON conversation(user_id);

-- 创建消息表
CREATE TABLE IF NOT EXISTS message (
    message_id SERIAL PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL REFERENCES conversation(conversation_id) ON DELETE CASCADE,
    sender_type VARCHAR(10) NOT NULL,
    content TEXT,
    message_type VARCHAR(20) DEFAULT 'text',
    status VARCHAR(20) DEFAULT 'sent',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_message_conversation_id ON message(conversation_id);

-- ==========================================
-- 第二部分: 深度学习模块 (v2)
-- ==========================================

-- Sequence for learning_sessions
CREATE SEQUENCE IF NOT EXISTS learning_sessions_id_seq INCREMENT 1 MINVALUE 1 MAXVALUE 2147483647 START 1 CACHE 1;

-- 学习会话表
CREATE TABLE IF NOT EXISTS learning_sessions (
    id INTEGER DEFAULT nextval('learning_sessions_id_seq') PRIMARY KEY,
    session_id VARCHAR(36) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(user_id),
    question TEXT NOT NULL,
    topic VARCHAR(200),
    subject_type VARCHAR(50),
    total_steps INTEGER DEFAULT 0,
    current_step INTEGER DEFAULT 0,
    status VARCHAR(50) DEFAULT 'in_progress',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_learning_sessions_user_id ON learning_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_learning_sessions_session_id ON learning_sessions(session_id);

-- Sequence for learning_steps
CREATE SEQUENCE IF NOT EXISTS learning_steps_id_seq INCREMENT 1 MINVALUE 1 MAXVALUE 2147483647 START 1 CACHE 1;

-- 学习步骤表
CREATE TABLE IF NOT EXISTS learning_steps (
    id INTEGER DEFAULT nextval('learning_steps_id_seq') PRIMARY KEY,
    session_id VARCHAR(36) NOT NULL REFERENCES learning_sessions(session_id) ON DELETE CASCADE,
    step_index INTEGER NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    status VARCHAR(50) DEFAULT 'pending',
    mastered_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_learning_steps_session_id ON learning_steps(session_id);
CREATE INDEX IF NOT EXISTS idx_learning_steps_step_index ON learning_steps(step_index);

-- Sequence for mastery_records
CREATE SEQUENCE IF NOT EXISTS mastery_records_id_seq INCREMENT 1 MINVALUE 1 MAXVALUE 2147483647 START 1 CACHE 1;

-- 掌握度记录表
CREATE TABLE IF NOT EXISTS mastery_records (
    id INTEGER DEFAULT nextval('mastery_records_id_seq') PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(user_id),
    knowledge_point VARCHAR(200) NOT NULL,
    topic VARCHAR(200),
    proficiency INTEGER DEFAULT 0,
    review_count INTEGER DEFAULT 0,
    last_reviewed_at TIMESTAMP,
    next_review_at TIMESTAMP,
    misconceptions TEXT[],
    status VARCHAR(50) DEFAULT 'learning',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, knowledge_point)
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_mastery_records_user_id ON mastery_records(user_id);

-- Sequence for learning_summaries
CREATE SEQUENCE IF NOT EXISTS learning_summaries_id_seq INCREMENT 1 MINVALUE 1 MAXVALUE 2147483647 START 1 CACHE 1;

-- 学习总结表
CREATE TABLE IF NOT EXISTS learning_summaries (
    id INTEGER DEFAULT nextval('learning_summaries_id_seq') PRIMARY KEY,
    session_id VARCHAR(36) UNIQUE NOT NULL REFERENCES learning_sessions(session_id) ON DELETE CASCADE,
    topic VARCHAR(200),
    question TEXT,
    total_time VARCHAR(50),
    completed_at TIMESTAMP,
    knowledge_graph TEXT,
    mastery_summary TEXT,
    misconceptions TEXT[],
    recommendations TEXT[],
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_learning_summaries_session_id ON learning_summaries(session_id);

-- ==========================================
-- 第二部分续: v3 新增表
-- ==========================================

CREATE TABLE IF NOT EXISTS note (
    id VARCHAR(32) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    subject VARCHAR(32),
    chapter VARCHAR(128),
    title VARCHAR(256) NOT NULL,
    content TEXT NOT NULL,
    tags VARCHAR(1024),
    file_type VARCHAR(16),
    image_url TEXT,
    chunk_count INT DEFAULT 0,
    linked_question_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS note_chunk (
    id VARCHAR(32) PRIMARY KEY,
    note_id VARCHAR(32) NOT NULL REFERENCES note(id),
    user_id BIGINT NOT NULL,
    chunk_index INT NOT NULL,
    knowledge_point VARCHAR(256),
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS question (
    id                VARCHAR(64) PRIMARY KEY,
    question_text     TEXT NOT NULL,
    question_type     VARCHAR(32) NOT NULL DEFAULT 'calculation',
    options           JSONB,
    correct_answer    TEXT NOT NULL,
    explanation       TEXT,
    difficulty        SMALLINT DEFAULT 3,
    subject           VARCHAR(32) NOT NULL,
    chapter           VARCHAR(128),
    knowledge_point   VARCHAR(256),
    tags              JSONB,
    year              INT,
    exam_type         VARCHAR(64),
    source            VARCHAR(32) DEFAULT 'official',
    linked_official_id VARCHAR(64),
    status            VARCHAR(20) DEFAULT 'published',
    created_at        TIMESTAMP DEFAULT NOW(),
    updated_at        TIMESTAMP DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_question_subject ON question(subject);
CREATE INDEX IF NOT EXISTS idx_question_year ON question(year);
CREATE INDEX IF NOT EXISTS idx_question_exam ON question(exam_type);
CREATE INDEX IF NOT EXISTS idx_question_kp ON question(knowledge_point);
CREATE INDEX IF NOT EXISTS idx_question_type ON question(question_type);
CREATE INDEX IF NOT EXISTS idx_question_diff ON question(difficulty);
CREATE INDEX IF NOT EXISTS idx_question_source ON question(source);
CREATE INDEX IF NOT EXISTS idx_question_tags ON question USING GIN(tags);
CREATE INDEX IF NOT EXISTS idx_question_text_gin ON question USING GIN(to_tsvector('simple', question_text));

CREATE TABLE IF NOT EXISTS user_answer_record (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    question_id     VARCHAR(64) NOT NULL REFERENCES question(id),
    selected_answer TEXT,
    is_correct      BOOLEAN,
    time_used       INT,
    source          VARCHAR(32),
    session_id      VARCHAR(64),
    created_at      TIMESTAMP DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_uqr_user ON user_answer_record(user_id);
CREATE INDEX IF NOT EXISTS idx_uqr_question ON user_answer_record(question_id);
CREATE INDEX IF NOT EXISTS idx_uqr_created ON user_answer_record(created_at);
CREATE UNIQUE INDEX IF NOT EXISTS idx_uqr_dedup ON user_answer_record(user_id, question_id, (created_at::date));

CREATE TABLE IF NOT EXISTS error_book (
    id VARCHAR(32) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    question_text TEXT,
    student_answer TEXT,
    correct_answer TEXT,
    knowledge_point VARCHAR(256),
    subject VARCHAR(32),
    error_type VARCHAR(32),
    diagnosis_json JSONB,
    similar_questions JSONB,
    review_level INT DEFAULT 0,
    next_review_at TIMESTAMP DEFAULT NOW(),
    mastered BOOLEAN DEFAULT FALSE,
    source_report_id VARCHAR(32),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS grading_report (
    id VARCHAR(32) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    total_score INT DEFAULT 0,
    max_score INT DEFAULT 0,
    correct_count INT DEFAULT 0,
    wrong_count INT DEFAULT 0,
    accuracy DECIMAL(5,2) DEFAULT 0,
    subject VARCHAR(32),
    status VARCHAR(16) DEFAULT 'completed',
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS grading_question_result (
    id VARCHAR(32) PRIMARY KEY,
    report_id VARCHAR(32) NOT NULL REFERENCES grading_report(id),
    question_index INT NOT NULL,
    question_text TEXT,
    student_answer TEXT,
    correct_answer TEXT,
    is_correct BOOLEAN,
    score INT DEFAULT 0,
    max_score INT DEFAULT 0,
    question_type VARCHAR(16),
    knowledge_point VARCHAR(256),
    diagnosis_json JSONB,
    similar_questions JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS student_behavior_log (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    action VARCHAR(32) NOT NULL,
    question_id VARCHAR(64),
    duration_ms BIGINT DEFAULT 0,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_behavior_user_id ON student_behavior_log(user_id);
CREATE INDEX IF NOT EXISTS idx_behavior_created_at ON student_behavior_log(created_at);

-- ==========================================
-- 第三部分: 初始化数据
-- ==========================================

-- 插入默认用户（如果不存在）
INSERT INTO users (account, password, email, telphone, nickname, status) 
VALUES ('admin', '123456', 'admin@example.com', '13800138000', '管理员', 1)
ON CONFLICT (account) DO NOTHING;
