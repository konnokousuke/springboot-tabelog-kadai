-- 会員情報テーブル
CREATE TABLE IF NOT EXISTS members (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL, -- 名前
    furigana VARCHAR(100) NOT NULL, -- フリガナ
    postal_code VARCHAR(10) NOT NULL, --郵便番号
    address VARCHAR(255) NOT NULL, -- 住所
    phone_number VARCHAR(20) NOT NULL, -- 電話番号
    email VARCHAR(100) NOT NULL UNIQUE, -- メールアドレスはユニーク
    customer_id VARCHAR(255) DEFAULT NULL, -- Stripeの顧客ID
    password VARCHAR(100) NOT NULL, -- パスワード
    role_id INT NOT NULL,
    enabled BOOLEAN NOT NULL, -- ユーザーが有効かどうか
    status ENUM('FREE', 'PAID') NOT NULL, -- 会員ステータス(無料か有料か)
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- メール認証テーブル
CREATE TABLE IF NOT EXISTS verification_tokens (
    id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    member_id INT NOT NULL UNIQUE,
    token VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES members (id) ON DELETE CASCADE -- カスケード削除
);

-- 管理者情報テーブル
CREATE TABLE IF NOT EXISTS admins (
    admin_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE, -- メールアドレスはユニーク
    password VARCHAR(100) NOT NULL, -- パスワード
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- ロール情報テーブル
CREATE TABLE IF NOT EXISTS roles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE -- ロール名はユニーク
);

-- 会員ロール情報テーブル（中間テーブル）
CREATE TABLE IF NOT EXISTS member_roles (
    member_id INT NOT NULL,
    role_id INT NOT NULL,
    FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE, -- カスケード削除
    FOREIGN KEY (role_id) REFERENCES roles(id),
    PRIMARY KEY (member_id, role_id)
);

-- カテゴリテーブル
CREATE TABLE IF NOT EXISTS categories (
    category_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, -- 主キーとしてのカテゴリID
    name VARCHAR(50) NOT NULL UNIQUE, -- カテゴリ名はユニーク
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 店舗情報テーブル
CREATE TABLE IF NOT EXISTS stores (
    store_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY, -- 主キーとしての店舗情報ID
    category_id INT, -- カテゴリID(外部キー)
    store_name VARCHAR(50) NOT NULL, -- 店舗名
    image_filename VARCHAR(255) NOT NULL UNIQUE, -- 画像ファイルはユニーク
    rating INT CHECK (rating BETWEEN 1 AND 5), -- レビュー(1から5の整数)
    description TEXT NOT NULL, -- 店舗説明
    price INT NOT NULL, -- 価格
    postal_code VARCHAR(20) NOT NULL, -- 郵便番号
    address VARCHAR(255) NOT NULL, -- 住所
    phone_number VARCHAR(20) NOT NULL, -- 電話番号
    opening_hours VARCHAR(50) NOT NULL, -- 営業時間
    closing_time TIME NOT NULL, -- 閉店時間 
    closed_days VARCHAR(50) NOT NULL, -- 定休日
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(category_id) -- 外部キー制約
);

-- 店舗情報とカテゴリの中間テーブル
CREATE TABLE IF NOT EXISTS store_category (
    store_id INT NOT NULL,
    category_id INT NOT NULL,
    PRIMARY KEY (store_id, category_id),
    FOREIGN KEY (store_id) REFERENCES stores(store_id),
    FOREIGN KEY (category_id) REFERENCES categories(category_id)
);

-- 予約テーブル
CREATE TABLE IF NOT EXISTS reservations (
    reservation_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    member_id INT NOT NULL, -- 会員ID(外部キー)
    store_id INT NOT NULL, -- 店舗情報ID(外部キー)
    reservation_datetime TIMESTAMP NOT NULL, -- 予約日時
    number_of_people INT NOT NULL, -- 予約人数
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE, -- カスケード削除
    FOREIGN KEY (store_id) REFERENCES stores(store_id)
);

-- レビューテーブル
CREATE TABLE IF NOT EXISTS reviews (
    review_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    member_id INT NOT NULL, -- 会員ID(外部キー)
    store_id INT NOT NULL, -- 店舗情報ID(外部キー)
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5), -- レビュー(1から5の整数)
    comment TEXT, -- コメント
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE, -- カスケード削除
    FOREIGN KEY (store_id) REFERENCES stores(store_id),
    UNIQUE (member_id, store_id) -- 同じ会員が同じ店舗に対して複数のレビューを持たないようにする
);

-- お気に入りテーブル
CREATE TABLE IF NOT EXISTS favorites (
    favorite_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    member_id INT NOT NULL, -- 会員ID(外部キー)
    store_id INT NOT NULL, -- 店舗情報ID(外部キー)
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE, -- カスケード削除
    FOREIGN KEY (store_id) REFERENCES stores(store_id),
    UNIQUE (member_id, store_id) -- 同じ会員が同じ店舗を複数回お気に入りに追加しないようにする
);