INSERT INTO public.roles (
    version_number,
    opal_domain_id,
    role_name,
    application_function_list
)
VALUES
(
   1,
    (
        SELECT opal_domain_id
        FROM public.domain
        WHERE opal_domain_name = 'User'
    ),
   'Role Mapping',
   ARRAY['ROLE_MAPPING']::t_permissions_enum[]
);
