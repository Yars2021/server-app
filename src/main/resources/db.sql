-- Create tables, constraints and relations
create table accounts
(
    id          integer not null,
    login       varchar(64),
    credentials varchar(128),
    primary key (id)
);
create unique index ui_accounts on accounts (login);
create sequence seq_accounts minvalue 1 increment 1 start 1 owned by accounts.id;

create table colors
(
    id    integer     not null,
    color varchar(32) not null,
    primary key (id)
);
create unique index ui_colors on colors (color);

create table countries
(
    id      integer     not null,
    country varchar(32) not null,
    primary key (id)
);
create unique index ui_countries on countries (country);

create table form_of_education
(
    id   integer     not null,
    form varchar(64) not null,
    primary key (id)
);
create unique index ui_form_of_education on form_of_education (form);

--     private String name; //Поле не может быть null, Строка не может быть пустой
--     private long height; //Значение поля должно быть больше 0
--     private Color hairColor; //Поле может быть null
--     private Country nationality; //Поле не может быть null
--     private Location location; //Поле может быть null
create table persons
(
    id            integer      not null,
    name          varchar(128) not null check ( length(trim(name)) > 0 ),
    height        integer check ( height > 0 ),
    color         integer,
    nationality   integer      not null,
    location_x    float        not null,
    location_y    float        not null,
    location_z    float        not null,
    location_name varchar(128),
    primary key (id),
    constraint fk_color foreign key (color) references colors (id),
    constraint fk_nationality foreign key (nationality) references countries (id)
);

create index idx_person on persons (id);
create unique index ui_person_name on persons (name);
create sequence seq_person minvalue 1 increment 1 start 1 owned by persons.id;

--     private long id = IdGenerator.getNextId(); //Значение поля должно быть больше 0, Значение этого поля должно быть уникальным, Значение этого поля должно генерироваться автоматически
--     private String name; //Поле не может быть null, Строка не может быть пустой
--     private Coordinates coordinates; //Поле не может быть null
--     private java.time.LocalDate creationDate = LocalDate.now(); //Поле не может быть null, Значение этого поля должно генерироваться автоматически
--     private Integer studentsCount; //Значение поля должно быть больше 0, Поле может быть null
--     private int shouldBeExpelled; //Значение поля должно быть больше 0
--     private int transferredStudents; //Значение поля должно быть больше 0
--     private FormOfEducation formOfEducation; //Поле не может быть null
--     private Person groupAdmin; //Поле может быть null
create table study_groups
(
    id                   integer          not null,
    name                 varchar(128)     not null check ( length(trim(name)) > 0 ),
    coord_x              integer,
    coord_y              double precision not null check ( coord_y <= 426 ),
    created              date             not null default current_date,
    creator              integer          not null,
    students_count       integer check ( students_count > 0 ),
    should_be_expelled   integer check ( should_be_expelled > 0 and should_be_expelled <= study_groups.students_count),
    transferred_students integer check ( transferred_students > 0 ),
    form_of_education    integer          not null,
    group_admin          integer,
    primary key (id),
    constraint fk_creator foreign key (creator) references accounts(id),
    constraint fk_form_of_education foreign key (form_of_education) references form_of_education (id),
    constraint fk_group_admin foreign key (group_admin) references persons (id)
);
create index idx_creation_date on study_groups (created);
create sequence seq_study_groups minvalue 1 increment 1 start 1 owned by study_groups.id;

-- Fill dictionaries
insert into colors
values (0, 'UNDEFINED'),
       (1, 'BLACK'),
       (2, 'BROWN'),
       (3, 'RED'),
       (4, 'WHITE');

insert into countries
values (0, 'UNDEFINED'),
       (1, 'AUSTRALIA'),
       (2, 'BELARUS'),
       (3, 'GABON'),
       (4, 'KAZAKHSTAN'),
       (5, 'KOREA_NORTH'),
       (6, 'KOREA_SOUTH'),
       (7, 'RUSSIA'),
       (8, 'SENEGAL'),
       (9, 'SERBIA'),
       (10, 'VIETNAM'),
       (11, 'ZIMBABWE');

insert into form_of_education
values (0, 'UNDEFINED'),
       (1, 'DISTANCE_EDUCATION'),
       (2, 'FULL_TIME_EDUCATION'),
       (3, 'EVENING_CLASSES');

-- Drop tables
-- drop table if exists study_groups;
-- drop table if exists persons;
-- drop table if exists accounts;
-- drop table if exists form_of_education cascade;
-- drop table if exists countries cascade;
-- drop table if exists colors cascade;