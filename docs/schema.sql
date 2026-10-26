-- Enterprise AI Business Suite - Core MySQL Schema (7 Tables)
CREATE DATABASE IF NOT EXISTS nexbusiness_db;
USE nexbusiness_db;

-- 1. Users Table (Accounts & Role-Based Access)
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    role ENUM('CUSTOMER', 'CASHIER', 'ADMIN') NOT NULL DEFAULT 'CUSTOMER',
    branch_location VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Products & Inventory Table (ERP / E-Commerce Stock)
CREATE TABLE IF NOT EXISTS products_inventory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(150) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    stock_qty INT NOT NULL DEFAULT 0,
    reorder_level INT NOT NULL DEFAULT 5,
    branch_id VARCHAR(50) NOT NULL
);

-- 3. POS Orders Table (POS / E-Commerce Orders)
CREATE TABLE IF NOT EXISTS pos_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    total_price DECIMAL(10, 2) NOT NULL,
    status ENUM('COMPLETED', 'PENDING', 'REFUNDED') DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (product_id) REFERENCES products_inventory(id)
);

-- 4. CRM Bookings & Tickets Table
CREATE TABLE IF NOT EXISTS crm_bookings_tickets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type ENUM('APPOINTMENT', 'SUPPORT_TICKET') NOT NULL,
    description TEXT NOT NULL,
    scheduled_time DATETIME,
    status VARCHAR(50) DEFAULT 'OPEN',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 5. Pending Approvals Table (Human-in-the-Loop Queue)
CREATE TABLE IF NOT EXISTS pending_approvals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    action_type ENUM('REFUND', 'PURCHASE_ORDER') NOT NULL,
    payload_json TEXT NOT NULL,
    requested_by_ai_session VARCHAR(100),
    status ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 6. User Memories Table (Long-Term Fact Memory)
CREATE TABLE IF NOT EXISTS user_memories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    memory_fact TEXT NOT NULL,
    extracted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 7. Knowledge Documents Metadata Table (SecureDoc RAG)
CREATE TABLE IF NOT EXISTS knowledge_documents (
    doc_id VARCHAR(100) PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    project_namespace VARCHAR(100) NOT NULL,
    access_role ENUM('PUBLIC', 'ADMIN_ONLY') NOT NULL DEFAULT 'PUBLIC',
    version INT NOT NULL DEFAULT 1,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Sample Seed Data (Colombo & Kandy Branches)
INSERT IGNORE INTO users (id, name, email, role, branch_location) VALUES
(1, 'Admin Manager', 'admin@nexbusiness.com', 'ADMIN', 'Colombo'),
(2, 'Kasun Perera', 'kasun@gmail.com', 'CUSTOMER', 'Kandy');

INSERT IGNORE INTO products_inventory (id, sku, name, price, stock_qty, reorder_level, branch_id) VALUES
(1, 'SKU-LAP-001', 'Enterprise Business Laptop', 245000.00, 12, 5, 'Colombo'),
(2, 'SKU-POS-002', 'Thermal Receipt Printer', 18500.00, 4, 5, 'Kandy');
