INSERT INTO local (id_local) VALUES
    ('LOCAL_OK'),
    ('LOCAL_EMPTY'),
    ('LOCAL_MULTI'),
    ('LOCAL_ROLLBACK'),
    ('LOCAL_CONCURRENT');

INSERT INTO surveillance (id_local, id_fiche) VALUES
    ('LOCAL_OK', 'FICHE_OK'),
    ('LOCAL_MULTI', 'FICHE_A'),
    ('LOCAL_MULTI', 'FICHE_B'),
    ('LOCAL_ROLLBACK', 'FICHE_ROLLBACK'),
    ('LOCAL_CONCURRENT', 'FICHE_CONCURRENT');
