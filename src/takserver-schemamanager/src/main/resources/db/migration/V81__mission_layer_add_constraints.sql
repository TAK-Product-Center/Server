alter table only mission_layer add constraint after_unique unique(after, parent_node_uid);
alter table only mission_layer add constraint uid_unique unique(uid);
