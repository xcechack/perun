insert into owners (id, name, type)
values (1, 'test_owner', 'test');

insert into services (id, name, owner_id)
values (1, 'test_service', 1);

insert into exec_services (id, service_id, default_delay, enabled, default_recurrence, script, type)
values (1, 1, 10, '1', 3, '/bin/true', 'SEND');