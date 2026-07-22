CREATE TABLE device (
                        id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                        name VARCHAR(255) NOT NULL,
                        ip_address VARCHAR(45),
                        status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                            ON UPDATE CURRENT_TIMESTAMP,
                        PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;
