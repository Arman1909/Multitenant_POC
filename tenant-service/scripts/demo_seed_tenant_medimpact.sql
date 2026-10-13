INSERT INTO tenant_medimpact.member
  (first_name, last_name, email, member_number, date_of_birth, insurance_number, carrier_hq_code, created_by, updated_by)
VALUES
  ('Alina',  'Novak',   'alina.novak@medimpact.example',   'MED-100001', '1984-02-11', 'INS-MED-661201', 'PHX001', 'seed', 'seed'),
  ('Derek',  'Wolfe',   'derek.wolfe@medimpact.example',   'MED-100002', '1991-10-08', 'INS-MED-661202', 'PHX002', 'seed', 'seed'),
  ('Farah',  'Haddad',  'farah.haddad@medimpact.example',  'MED-200001', '1987-06-19', 'INS-MED-661301', 'DEN001', 'seed', 'seed'),
  ('Marcus', 'Boyd',    'marcus.boyd@medimpact.example',   'MED-200002', '1993-03-27', 'INS-MED-661302', 'DEN002', 'seed', 'seed'),
  ('Ines',   'Dubois',  'ines.dubois@medimpact.example',   'MED-200003', '1980-09-02', 'INS-MED-661303', 'DEN003', 'seed', 'seed'),
  ('Tobias', 'Lindgren','tobias.lindgren@medimpact.example','MED-300001', '1989-12-15', 'INS-MED-661401', 'ATL001', 'seed', 'seed'),
  ('Renee',  'Carter',  'renee.carter@medimpact.example',  'MED-300002', '1996-01-30', 'INS-MED-661402', 'ATL002', 'seed', 'seed')
ON CONFLICT (email) DO NOTHING;