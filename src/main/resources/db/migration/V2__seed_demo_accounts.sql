INSERT INTO accounts (id, code, name, type, currency, created_at) VALUES
    ('11111111-1111-1111-1111-111111111111', 'CASH-USD', 'USD operating cash', 'ASSET', 'USD', now()),
    ('22222222-2222-2222-2222-222222222222', 'CASH-EUR', 'EUR operating cash', 'ASSET', 'EUR', now()),
    ('33333333-3333-3333-3333-333333333333', 'DEPOSITS-USD', 'Customer deposits USD', 'LIABILITY', 'USD', now()),
    ('44444444-4444-4444-4444-444444444444', 'FEE-INCOME-USD', 'Fee income USD', 'REVENUE', 'USD', now()),
    ('55555555-5555-5555-5555-555555555555', 'CLEARING-USD', 'Settlement clearing USD', 'ASSET', 'USD', now());
