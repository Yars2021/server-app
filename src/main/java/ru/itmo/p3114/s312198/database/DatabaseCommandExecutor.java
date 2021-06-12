package ru.itmo.p3114.s312198.database;

import org.postgresql.util.PSQLException;
import ru.itmo.p3114.s312198.commands.CommandRecord;
import ru.itmo.p3114.s312198.commands.actions.AbstractCommand;
import ru.itmo.p3114.s312198.commands.actions.complex.AbstractComplexCommand;
import ru.itmo.p3114.s312198.commands.markers.CollectionInteracting;
import ru.itmo.p3114.s312198.commands.markers.DatabaseInteracting;
import ru.itmo.p3114.s312198.commands.results.CommandResult;
import ru.itmo.p3114.s312198.commands.results.Status;
import ru.itmo.p3114.s312198.exceptions.InvalidCommandException;
import ru.itmo.p3114.s312198.managers.CommandExecutor;
import ru.itmo.p3114.s312198.managers.SynchronizedCollectionManager;
import ru.itmo.p3114.s312198.structures.StudyGroup;
import ru.itmo.p3114.s312198.transmission.User;

import java.util.ArrayList;

public class DatabaseCommandExecutor extends CommandExecutor {
    public DatabaseCommandExecutor(SynchronizedCollectionManager synchronizedCollectionManager) {
        super(synchronizedCollectionManager);
    }

    public CommandResult applyToDB(AbstractCommand command, User actor) throws InvalidCommandException {
        if (command == null) {
            throw new InvalidCommandException("No command found");
        } else {
            if (command instanceof DatabaseInteracting) {
                Long id;
                Boolean max = Boolean.TRUE;
                try (DBHelper dbHelper = new DBHelper()) {
                    switch (command.getCommandName()) {
                        case "add":
                            id = dbHelper.createStudyGroup(((AbstractComplexCommand) command).getComplexArgument());
                            ((AbstractComplexCommand) command).getComplexArgument().setId(id);
                            System.out.println("   ID: " + id);
                            return synchronizedCollectionManager.applyToCollection(command);
                        case "add_if_max":
                            for (StudyGroup studyGroup : synchronizedCollectionManager.getCollection()) {
                                if (studyGroup.compareTo(((AbstractComplexCommand) command).getComplexArgument()) > 0) {
                                    max = Boolean.FALSE;
                                    break;
                                }
                            }
                            if (max) {
                                id = dbHelper.createStudyGroup(((AbstractComplexCommand) command).getComplexArgument());
                                ((AbstractComplexCommand) command).getComplexArgument().setId(id);
                            }
                            return synchronizedCollectionManager.applyToCollection(command);
                        case "remove_greater":
                            for (StudyGroup studyGroup : synchronizedCollectionManager.getCollection()) {
                                if (actor.getId().equals(studyGroup.getId()) &&
                                        studyGroup.compareTo(((AbstractComplexCommand) command).getComplexArgument()) > 0) {
                                    dbHelper.removeStudyGroupByID(studyGroup.getId());
                                }
                            }
                            return synchronizedCollectionManager.applyToCollection(command);
                        case "update":
                            try {
                                dbHelper.updateByID(Long.parseLong(command.getArguments().get(0)),
                                        ((AbstractComplexCommand) command).getComplexArgument(), actor);
                                return synchronizedCollectionManager.applyToCollection(command);
                            } catch (NumberFormatException numberFormatException) {
                                ArrayList<String> output = new ArrayList<>();
                                output.add("Invalid command argument format");
                                return new CommandResult(Status.INCORRECT_ARGUMENTS, output);
                            }
                        case "clear":
                            for (StudyGroup studyGroup : synchronizedCollectionManager.getCollection()) {
                                if (actor.getId().equals(studyGroup.getId())) {
                                    dbHelper.removeStudyGroupByID(studyGroup.getId());
                                }
                            }
                            return synchronizedCollectionManager.applyToCollection(command);
                        case "remove_all_by_should_be_expelled":
                            try {
                                for (StudyGroup studyGroup : synchronizedCollectionManager.getCollection()) {
                                    if (actor.getId().equals(studyGroup.getId()) &&
                                            studyGroup.getShouldBeExpelled().equals(Integer.parseInt(command.getArguments().get(0)))) {
                                        dbHelper.removeStudyGroupByID(studyGroup.getId());
                                    }
                                }
                                return synchronizedCollectionManager.applyToCollection(command);
                            } catch (NumberFormatException numberFormatException) {
                                ArrayList<String> output = new ArrayList<>();
                                output.add("Invalid command argument format");
                                return new CommandResult(Status.INCORRECT_ARGUMENTS, output);
                            }
                        case "remove_any_by_transferred_students":
                            try {
                                for (StudyGroup studyGroup : synchronizedCollectionManager.getCollection()) {
                                    if (actor.getId().equals(studyGroup.getId()) &&
                                            studyGroup.getTransferredStudents().equals(Integer.parseInt(command.getArguments().get(0)))) {
                                        dbHelper.removeStudyGroupByID(studyGroup.getId());
                                        break;
                                    }
                                }
                                return synchronizedCollectionManager.applyToCollection(command);
                            } catch (NumberFormatException numberFormatException) {
                                ArrayList<String> output = new ArrayList<>();
                                output.add("Invalid command argument format");
                                return new CommandResult(Status.INCORRECT_ARGUMENTS, output);
                            }
                        case "remove_by_id":
                            try {
                                for (StudyGroup studyGroup : synchronizedCollectionManager.getCollection()) {
                                    if (actor.getId().equals(studyGroup.getId()) &&
                                            studyGroup.getId().equals(Long.parseLong(command.getArguments().get(0)))) {
                                        dbHelper.removeStudyGroupByID(studyGroup.getId());
                                    }
                                }
                                return synchronizedCollectionManager.applyToCollection(command);
                            } catch (NumberFormatException numberFormatException) {
                                ArrayList<String> output = new ArrayList<>();
                                output.add("Invalid command argument format");
                                return new CommandResult(Status.INCORRECT_ARGUMENTS, output);
                            }
                        default:
                            throw new InvalidCommandException("Invalid command type");
                    }
                } catch (PSQLException psqlException) {
                    throw new InvalidCommandException("Unable to connect");
                }
            }
        }
        ArrayList<String> output = new ArrayList<>();
        output.add("Invalid command argument format");
        return new CommandResult(Status.INCORRECT_ARGUMENTS, output);
    }

    public CommandResult executeCommand(AbstractCommand command) throws InvalidCommandException {
        if (command == null) {
            throw new InvalidCommandException("No command found");
        } else {
            if (!(command instanceof CollectionInteracting)) {
                return command.execute();
            } else {
                throw new InvalidCommandException("This command interacts with the collection");
            }
        }
    }

    public CommandResult executeCommandRecord(CommandRecord commandRecord) throws InvalidCommandException {
        AbstractCommand command = commandRecord.getCommand();
        if (command instanceof CollectionInteracting) {
            return synchronizedCollectionManager.applyToCollection(command);
        } else {
            return executeCommand(command);
        }
    }

    public CommandResult executeCommandRecord(CommandRecord commandRecord, User actor) throws InvalidCommandException {
        AbstractCommand command = commandRecord.getCommand();
        if (command instanceof CollectionInteracting) {
            if (command instanceof DatabaseInteracting) {
                return applyToDB(command, actor);
            }
            return synchronizedCollectionManager.applyToCollection(command);
        } else {
            return executeCommand(command);
        }
    }
}
