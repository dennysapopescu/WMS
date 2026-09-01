-- Standardize legacy Romanian location descriptions to English
UPDATE locations SET description = 'High-Value Electronics' WHERE code = 'A-01-01' OR description LIKE '%Electronice High-Value%';
UPDATE locations SET description = 'Electronics - Accessories' WHERE code = 'A-01-02' OR description LIKE '%Electronice - Accesorii%';
UPDATE locations SET description = 'Bulky & Oversized Goods' WHERE code = 'B-22-01' OR description LIKE '%Produse Voluminoase%';
UPDATE locations SET description = 'Fragile Goods (Glassware)' WHERE code = 'FRG-01' OR description LIKE '%Produse Fragile%' OR description LIKE '%Sticl%';
UPDATE locations SET description = 'Returns & Inspection Zone' WHERE code = 'RET-01' OR description LIKE '%Retururi%' OR description LIKE '%Verificare%';
