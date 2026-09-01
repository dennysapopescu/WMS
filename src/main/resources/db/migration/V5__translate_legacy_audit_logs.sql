-- Standardize legacy Romanian audit action names to English
UPDATE inventory_log SET action = 'ADD' WHERE action IN ('ADĂUGARE', 'ADAUGARE');
UPDATE inventory_log SET action = 'UPDATE' WHERE action = 'MODIFICARE';
UPDATE inventory_log SET action = 'STOCK_REDUCTION' WHERE action = 'REDUCERE';
UPDATE inventory_log SET action = 'DELETE' WHERE action IN ('ȘTERGERE', 'STERGERE');
UPDATE inventory_log SET action = 'IMPORT (REDIRECTED)' WHERE action IN ('IMPORT (REDIRECȚIONAT)', 'IMPORT (REDIRECTIONAT)', 'IMPORT (REDIRECÈIONAT)');
UPDATE inventory_log SET action = REPLACE(action, 'TRANSFER către ', 'TRANSFER to ') WHERE action LIKE 'TRANSFER către %' OR action LIKE 'TRANSFER c%tre %';
UPDATE inventory_log SET action = REPLACE(action, 'TRANSFER catre ', 'TRANSFER to ') WHERE action LIKE 'TRANSFER catre %';
UPDATE inventory_log SET action = REPLACE(action, 'PICKING FINALIZAT', 'PICKING_COMPLETED') WHERE action LIKE '%PICKING FINALIZAT%';
