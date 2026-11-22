-- Built-in legacy codes are unique per profile. Custom types have no legacy code.
-- Both indexed expressions are null for custom types, so Oracle omits those rows.
create unique index ux_basic_org_type_seed_code on basic_organization_type (
    case when legacy_code is not null then profile_id end,
    legacy_code
);

drop index ux_basic_org_type_legacy;
