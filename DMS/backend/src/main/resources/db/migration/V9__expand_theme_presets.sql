ALTER TABLE user_preference DROP CONSTRAINT ck_user_preference_theme_preset;
ALTER TABLE user_preference ADD CONSTRAINT ck_user_preference_theme_preset
    CHECK (theme_preset IN ('blue', 'indigo', 'teal', 'emerald', 'violet', 'amber', 'gray', 'brown', 'pink', 'red'));
