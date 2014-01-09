insert into owners (id, name, type)
values (1, 'test_owner', 'test');

insert into services (id, name, owner_id)
values (1, 'test_service', 1);

insert into exec_services (id, service_id, default_delay, enabled, default_recurrence, script, type)
values (1, 1, 10, '1', 3, '/bin/true', 'SEND');

insert into exec_services (id, service_id, default_delay, enabled, default_recurrence, script, type)
values (2, 1, 10, '1', 3, '/bin/true', 'SEND');

insert into facilities (id, name, type)
values (0, 'testFacility', 'host');

insert into engines (id, ip_address, port)
values (0, '127.0.0.1', 5560);

insert into tasks (id, exec_service_id, facility_id, schedule, recurrence, delay, status, engine_id)
values (1, 1, 0, now, 5, 5, 'NONE', 0);

insert into tasks (id, exec_service_id, facility_id, schedule, recurrence, delay, status, engine_id)
values (2, 2, 0, now, 5, 5, 'NONE', 0);

insert into destinations (id, destination, type) 
values (1, 'par_dest1', 'PARALLEL');

insert into destinations (id, destination, type) 
values (2, 'par_dest2', 'PARALLEL');

insert into destinations (id, destination, type) 
values (3, 'par_dest3', 'ONE');

insert into destinations (id, destination, type) 
values (4, 'par_dest4', 'ONE');
