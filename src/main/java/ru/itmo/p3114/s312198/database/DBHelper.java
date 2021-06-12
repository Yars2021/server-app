package ru.itmo.p3114.s312198.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.itmo.p3114.s312198.exceptions.InvalidCredentialsException;
import ru.itmo.p3114.s312198.exceptions.RegistrationException;
import ru.itmo.p3114.s312198.structures.Color;
import ru.itmo.p3114.s312198.structures.Coordinates;
import ru.itmo.p3114.s312198.structures.Country;
import ru.itmo.p3114.s312198.structures.FormOfEducation;
import ru.itmo.p3114.s312198.structures.Location;
import ru.itmo.p3114.s312198.structures.Person;
import ru.itmo.p3114.s312198.structures.StudyGroup;
import ru.itmo.p3114.s312198.structures.builders.LocationBuilder;
import ru.itmo.p3114.s312198.structures.builders.PersonBuilder;
import ru.itmo.p3114.s312198.structures.builders.StudyGroupBuilder;
import ru.itmo.p3114.s312198.transmission.User;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.LinkedHashSet;
import java.util.Properties;

public class DBHelper implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(DBHelper.class);

    private final HikariDataSource dataSource;

    public DBHelper() throws PSQLException {
        Properties properties = new Properties();
        try (BufferedReader reader = new BufferedReader(new FileReader("/home/s312198/semester2/lab72_dir/db.properties"))) {
            properties.load(reader);
        } catch (FileNotFoundException fileNotFoundException) {
            logger.error(fileNotFoundException.getMessage());
        } catch (IOException ioException) {
            logger.error("An unexpected IOException occurred");
        }
        HikariConfig config = new HikariConfig(properties);
        dataSource = new HikariDataSource(config);
    }

    public Long registerAccount(User user) throws RegistrationException {
        if (user != null) {
            try (Connection connection = dataSource.getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO accounts (id, login, credentials) VALUES(nextval('seq_accounts'), ?, ?) RETURNING id")) {
                    statement.setString(1, user.getUsername());
                    statement.setString(2, user.getCredentials());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            return resultSet.getLong(1);
                        }
                    }
                }
            } catch (SQLException sqlException) {
                logger.error(sqlException.getMessage());
            }
        }
        throw new RegistrationException();
    }

    public Long validateAccount(User user) throws InvalidCredentialsException {
        if (user != null) {
            try (Connection connection = dataSource.getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT id FROM accounts WHERE login = ? AND credentials = ?")) {
                    statement.setString(1, user.getUsername());
                    statement.setString(2, user.getCredentials());
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            return resultSet.getLong(1);
                        }
                    }
                }
            } catch (SQLException sqlException) {
                logger.error(sqlException.getMessage());
            }
        }
        throw new InvalidCredentialsException();
    }

    public String getCreatorNameByID(Long id) {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT login FROM accounts WHERE id = ?")) {
                statement.setLong(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getString(1);
                    }
                }
            }
        } catch (SQLException sqlException) {
            logger.error(sqlException.getMessage());
        }
        return null;
    }

    public Long createPerson(Person person) {
        if (person == null) {
            return null;
        } else {
            try (Connection connection = dataSource.getConnection()) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO persons " +
                                "(id, name, height, color, nationality, " +
                                "location_x, location_y, " +
                                "location_z, location_name) " +
                                "VALUES (nextval('seq_person'), ?, ?, ?, ?, ?, ?, ?, ?) " +
                                "RETURNING id")) {
                    statement.setString(1, person.getName());
                    statement.setLong(2, person.getHeight());
                    statement.setDouble(3, person.getHairColor().getKey());
                    statement.setLong(4, person.getNationality().getKey());
                    if (person.getLocation() == null) {
                        statement.setFloat(5, 0);
                        statement.setFloat(6, 0);
                        statement.setFloat(7, 0);
                        statement.setString(8, null);
                    } else {
                        statement.setFloat(5, person.getLocation().getX());
                        statement.setFloat(6, person.getLocation().getY());
                        statement.setFloat(7, person.getLocation().getZ());
                        statement.setString(8, person.getLocation().getName());
                    }
                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (resultSet.next()) {
                            return resultSet.getLong(1);
                        }
                    }
                }
            } catch (SQLException sqlException) {
                logger.error(sqlException.getMessage());
            }
        }
        return null;
    }

    public Long createStudyGroup(StudyGroup studyGroup) {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO study_groups " +
                            "(id, name, coord_x, coord_y, creator, " +
                            "students_count, should_be_expelled, " +
                            "transferred_students, form_of_education, " +
                            "group_admin) " +
                            "VALUES (nextval('seq_study_groups'), ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                            "RETURNING id")) {
                statement.setString(1, studyGroup.getName());
                statement.setLong(2, studyGroup.getCoordinates().getX());
                statement.setDouble(3, studyGroup.getCoordinates().getY());
                statement.setLong(4, studyGroup.getCreatorId());
                statement.setInt(5, studyGroup.getStudentsCount());
                statement.setInt(6, studyGroup.getShouldBeExpelled());
                statement.setInt(7, studyGroup.getTransferredStudents());
                statement.setLong(8, studyGroup.getFormOfEducation().getKey());
                if (createPerson(studyGroup.getGroupAdmin()) == null) {
                    statement.setNull(9, Types.INTEGER);
                } else {
                    statement.setLong(9, createPerson(studyGroup.getGroupAdmin()));
                }
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getLong(1);
                    }
                }
            }
        } catch (SQLException sqlException) {
            logger.error(sqlException.getMessage());
        }
        return null;
    }

    public void removeStudyGroupByID(Long id) {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM study_groups WHERE id = ?")) {
                statement.setLong(1, id);
                statement.executeQuery();
            }
        } catch (SQLException sqlException) {
            logger.error(sqlException.getMessage());
        }
    }

    public void updateByID(Long id, StudyGroup studyGroup, User actor) {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("UPDATE study_groups " +
                    "    SET name = ?," +
                    "        coord_x = ?," +
                    "        coord_y = ?," +
                    "        students_count = ?," +
                    "        should_be_expelled = ?," +
                    "        transferred_students = ?," +
                    "        form_of_education = ?," +
                    "        group_admin = ?" +
                    "  WHERE id = ?" +
                    "    AND creator = ?")) {
                statement.setString(1, studyGroup.getName());
                statement.setLong(2, studyGroup.getCoordinates().getX());
                statement.setDouble(3, studyGroup.getCoordinates().getY());
                statement.setInt(4, studyGroup.getStudentsCount());
                statement.setInt(5, studyGroup.getShouldBeExpelled());
                statement.setInt(6, studyGroup.getTransferredStudents());
                statement.setLong(7, studyGroup.getFormOfEducation().getKey());
                if (studyGroup.getGroupAdmin() == null) {
                    statement.setNull(8, Types.INTEGER);
                } else {
                    statement.setLong(8, studyGroup.getGroupAdmin().getId());
                }
                statement.setLong(9, id);
                statement.setLong(10, actor.getId());
            }
        } catch (SQLException sqlException) {
            logger.error(sqlException.getMessage());
        }
    }

    public Person getAdminByID(Long id) {
        Person person;
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT " +
                            "name, height, color, nationality, " +
                            "location_x, location_y, location_z, " +
                            "location_name " +
                            "FROM persons WHERE id = ?")) {
                statement.setLong(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        Location location;
                        if (resultSet.getFloat(5) == 0 && resultSet.getFloat(6) == 0 &&
                                resultSet.getFloat(7) == 0 && resultSet.getString(8) == null) {
                            location = null;
                        } else {
                            location = new LocationBuilder()
                                    .addX(resultSet.getFloat(5))
                                    .addY(resultSet.getFloat(6))
                                    .addZ(resultSet.getFloat(7))
                                    .addName(resultSet.getString(8))
                                    .toLocation();
                        }

                        return new PersonBuilder()
                                .addId(id)
                                .addName(resultSet.getString(1))
                                .addHeight(resultSet.getInt(2))
                                .addHairColor(Color.valueOf(resultSet.getLong(3)))
                                .addNationality(Country.valueOf(resultSet.getLong(4)))
                                .addLocation(location)
                                .toPerson();
                    }
                }
            }
        } catch (SQLException sqlException) {
            logger.error(sqlException.getMessage());
        }
        return null;
    }

    public LinkedHashSet<StudyGroup> getStudyGroups() {
        LinkedHashSet<StudyGroup> collection = new LinkedHashSet<>();
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT " +
                            "id, name, coord_x, coord_y, created, creator, " +
                            "students_count, should_be_expelled, transferred_students, " +
                            "form_of_education, group_admin " +
                            "FROM study_groups")) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        collection.add(new StudyGroupBuilder()
                                .addId(resultSet.getLong(1))
                                .addName(resultSet.getString(2))
                                .addCoordinates(new Coordinates(resultSet.getLong(3), resultSet.getDouble(4)))
                                .addCreationDate(resultSet.getDate(5).toLocalDate())
                                .addOwner(getCreatorNameByID(resultSet.getLong(6)))
                                .addCreatorId(resultSet.getLong(6))
                                .addStudentsCount(resultSet.getInt(7))
                                .addShouldBeExpelled(resultSet.getInt(8))
                                .addTransferredStudents(resultSet.getInt(9))
                                .addFormOfEducation(FormOfEducation.valueOf(resultSet.getLong(10)))
                                .addGroupAdmin(getAdminByID(resultSet.getLong(11)))
                                .toStudyGroup());
                    }
                }
            }
        } catch (SQLException sqlException) {
            logger.error(sqlException.getMessage());
        }
        return collection;
    }

    @Override
    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
