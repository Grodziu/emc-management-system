ALTER TABLE projects
    ADD COLUMN start_date DATE NULL AFTER te_id,
    ADD COLUMN end_date DATE NULL AFTER start_date;

UPDATE projects p
LEFT JOIN (
    SELECT
        pl.project_id,
        MIN(pl.start_date) AS earliest_start,
        CASE
            WHEN COUNT(pl.id) > 0
                 AND SUM(CASE WHEN vls.leg_status IN ('PASSED', 'FAILED') THEN 1 ELSE 0 END) = COUNT(pl.id)
                 AND SUM(CASE WHEN pl.end_date IS NOT NULL THEN 1 ELSE 0 END) = COUNT(pl.id)
                THEN MAX(pl.end_date)
            ELSE NULL
        END AS latest_end
    FROM project_legs pl
    LEFT JOIN vw_leg_status vls ON vls.leg_id = pl.id
    GROUP BY pl.project_id
) summary ON summary.project_id = p.id
SET
    p.start_date = summary.earliest_start,
    p.end_date = summary.latest_end;
