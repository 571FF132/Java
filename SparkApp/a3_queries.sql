--The total gifts by year and fund. This should be sorted first by year, then by fund ID.
-- Return the year, fund ID, fund name, and total gifts.
SELECT EXTRACT(YEAR FROM gift_date) AS year, fund_id, fund_name, COUNT(gift_id) AS gifts
FROM gift
  JOIN gift_fund_allocation USING (gift_id)
  JOIN fund USING (fund_id)
  GROUP BY  year, fund_id, fund_name
  ORDER BY year, fund_id;

-- The ‘top donors’ list: the 10 donors with the highest lifetime contributions across all funds.
-- Return their donor ID, name, and total gifts.
SELECT donor.donor_id, donor_name, COUNT(gift_id) as total_gifts
FROM donor
  LEFT JOIN gift ON (gift.donor_id = donor.donor_id)
  GROUP BY donor.donor_id, donor_name
  ORDER BY total_gifts DESC
  LIMIT 10;

-- For the year 2013, the donors to the ‘Veterinary Assistance’ fund
-- with their total contributions to that fund for the year.
-- List the donor ID, name, and funds contributed in 2013 to Veterinary Assistance.
SELECT donor.donor_id, donor_name, SUM(gift_fund_allocation.amount) AS amount
FROM donor
  JOIN gift ON (gift.donor_id = donor.donor_id)
  JOIN gift_fund_allocation ON (gift.gift_id = gift_fund_allocation.gift_id)
  JOIN fund ON (fund_name ='Veterinary Assistance')
  WHERE (EXTRACT(YEAR FROM gift_date) > 2012)
GROUP BY donor.donor_id;

-- The donors who have donated at least $10,000 since January 1, 2010
-- (donor ID, name, total gifts).
SELECT donor_id, donor_name, total_gifts
FROM (SELECT donor.donor_id, donor_name, SUM(amount) as total_gifts
  FROM donor
  JOIN gift ON (EXTRACT(YEAR FROM gift_date) > 2010 AND donor.donor_id = gift.donor_id)
  JOIN gift_fund_allocation USING (gift_id)
  GROUP BY donor.donor_id
  ORDER BY total_gifts DESC) AS donor
WHERE total_gifts>10000;

-- The names and email addresses of all donors wo contributed in the year 2012
-- (with no duplicates)
SELECT donor_name, donor_email
  FROM donor
  JOIN gift ON(EXTRACT(YEAR FROM gift_date) = 2012 AND donor.donor_id = gift.donor_id)
GROUP BY donor_name, donor_email;

