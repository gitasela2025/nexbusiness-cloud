-- Demo reset: Paper Cups is low on stock, no old orders or approval requests.
UPDATE products_inventory SET stock_qty = 3 WHERE sku = 'SKU-002';
DELETE FROM pending_approvals;
DELETE FROM pos_orders;
ALTER TABLE pending_approvals AUTO_INCREMENT = 1;
ALTER TABLE pos_orders AUTO_INCREMENT = 1;
SELECT id, name, stock_qty, reorder_level FROM products_inventory;
