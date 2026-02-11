-- Customer Identity Table
CREATE TABLE customer_identity (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    email_normalized VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    registration_method VARCHAR(20) NOT NULL,
    oauth_provider VARCHAR(20),
    oauth_provider_id VARCHAR(255),
    oauth_token_encrypted TEXT,
    email_verified BOOLEAN DEFAULT FALSE,
    verification_token_hash VARCHAR(255),
    verification_token_expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP,
    
    CONSTRAINT check_registration_method CHECK (registration_method IN ('email', 'oauth')),
    CONSTRAINT check_oauth_provider CHECK (
        (registration_method = 'oauth' AND oauth_provider IS NOT NULL) OR
        (registration_method = 'email' AND oauth_provider IS NULL)
    ),
    CONSTRAINT check_password_hash CHECK (
        (registration_method = 'email' AND password_hash IS NOT NULL) OR
        (registration_method = 'oauth' AND password_hash IS NULL)
    )
);

-- Indexes for performance
CREATE INDEX idx_email_normalized ON customer_identity(email_normalized);
CREATE INDEX idx_oauth_provider_id ON customer_identity(oauth_provider, oauth_provider_id);
CREATE INDEX idx_verification_token ON customer_identity(verification_token_hash) WHERE email_verified = FALSE;

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger to automatically update updated_at
CREATE TRIGGER update_customer_identity_updated_at BEFORE UPDATE
    ON customer_identity FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
