-- Sample citizens
INSERT INTO citizens (citizen_id, citizen_code, first_name, last_name, national_id, email, phone, address, created_at, updated_at)
VALUES 
('550e8400-e29b-41d4-a716-446655440000', 'CIT-001', 'John', 'Doe', 'BE123456789', 'john.doe@example.com', '+32123456789', '123 Main St, Brussels', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('550e8400-e29b-41d4-a716-446655440001', 'CIT-002', 'Jane', 'Smith', 'BE987654321', 'jane.smith@example.com', '+32987654321', '456 Oak Ave, Antwerp', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Sample tax credits
INSERT INTO tax_credits (tax_credit_id, citizen_id, total_credit, allocated_credit, currency, version, created_at, updated_at)
VALUES 
('660e8400-e29b-41d4-a716-446655440000', '550e8400-e29b-41d4-a716-446655440000', 0.00, 0.00, 'EUR', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('660e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440001', 0.00, 0.00, 'EUR', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Sample debts
INSERT INTO debts (debt_id, debt_code, citizen_id, debt_type, original_amount, outstanding_amount, currency, due_date, status, priority, structured_reference, version, created_at, updated_at)
VALUES 
('770e8400-e29b-41d4-a716-446655440000', '2026-000001', '550e8400-e29b-41d4-a716-446655440000', 'TAX', 1500.00, 1500.00, 'EUR', '2026-12-31', 'OPEN', 1, '202600000100', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('770e8400-e29b-41d4-a716-446655440001', '2026-000002', '550e8400-e29b-41d4-a716-446655440000', 'PENALTY', 250.00, 250.00, 'EUR', '2026-06-30', 'OPEN', 2, '202600000200', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('770e8400-e29b-41d4-a716-446655440002', '2026-000003', '550e8400-e29b-41d4-a716-446655440001', 'TAX', 2000.00, 2000.00, 'EUR', '2026-12-31', 'OPEN', 1, '202600000300', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Made with Bob
