alter table data_feed add column groups bit varying;
update data_feed set groups = rpad('', 32768, '1')::bit(32768)::bit varying where groups is null or groups = '';