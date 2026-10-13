INSERT INTO tenant_laker.member
  (first_name, last_name, email, member_number, date_of_birth, insurance_number, carrier_hq_code, created_by, updated_by)
VALUES
  ('Kobe',  'Adams',  'kobe.adams@laker.example',  'LAK-100001', '1985-03-14', 'INS-LAK-778201', 'MBR001', 'seed', 'seed'),
  ('Maya',  'Chen',   'maya.chen@laker.example',   'LAK-100002', '1990-07-22', 'INS-LAK-778202', 'MBR002', 'seed', 'seed'),
  ('Ravi',  'Patel',  'ravi.patel@laker.example',  'LAK-100003', '1978-11-05', 'INS-LAK-778203', 'MBR003', 'seed', 'seed'),
  ('Julia', 'Ortiz',  'julia.ortiz@laker.example', 'LAK-200001', '1982-01-30', 'INS-LAK-551101', 'CHA001', 'seed', 'seed'),
  ('Noah',  'Kim',    'noah.kim@laker.example',    'LAK-200002', '1995-09-12', 'INS-LAK-551102', 'CHA002', 'seed', 'seed'),
  ('Priya', 'Nair',   'priya.nair@laker.example',  'LAK-200003', '1988-05-19', 'INS-LAK-551103', 'CHA003', 'seed', 'seed'),
  ('Owen',  'Diaz',   'owen.diaz@laker.example',   'LAK-200004', '1991-06-23', 'INS-LAK-551104', 'CHA004', 'seed', 'seed'),
  ('Liam',  'Foster', 'liam.foster@laker.example', 'LAK-300001', '1979-12-02', 'INS-LAK-330901', 'KHO001', 'seed', 'seed'),
  ('Sara',  'Nguyen', 'sara.nguyen@laker.example',  'LAK-300002', '1993-04-27', 'INS-LAK-330902', 'KHO002', 'seed', 'seed'),
  ('Ethan', 'Brooks', 'ethan.brooks@laker.example', 'LAK-300003', '1987-08-15', 'INS-LAK-330903', 'KHO003', 'seed', 'seed')
ON CONFLICT (email) DO NOTHING;