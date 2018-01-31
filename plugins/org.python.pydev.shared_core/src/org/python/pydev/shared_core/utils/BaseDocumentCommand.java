package org.python.pydev.shared_core.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;

/**
 * Represents a text modification as a document replace command. The text
 * modification is given as a {@link org.eclipse.swt.events.VerifyEvent} and
 * translated into a document replace command relative to a given offset. A
 * document command can also be used to initialize a given
 * <code>VerifyEvent</code>.
 * <p>
 * A document command can also represent a list of related changes.</p>
 */
public class BaseDocumentCommand implements IDocumentCommand {

    /**
     * A command which is added to document commands.
     * @since 2.1
     */
    private static class Command implements Comparable<Command> {
        /** The offset of the range to be replaced */
        private final int fOffset;
        /** The length of the range to be replaced. */
        private final int fLength;
        /** The replacement text */
        private final String fText;
        /** The listener who owns this command */
        private final IDocumentListener fOwner;

        /**
         * Creates a new command with the given specification.
         *
         * @param offset the offset of the replace command
         * @param length the length of the replace command
         * @param text the text to replace with, may be <code>null</code>
         * @param owner the document command owner, may be <code>null</code>
         * @since 3.0
         */
        public Command(int offset, int length, String text, IDocumentListener owner) {
            if (offset < 0 || length < 0) {
                throw new IllegalArgumentException();
            }
            fOffset = offset;
            fLength = length;
            fText = text;
            fOwner = owner;
        }

        /**
         * Executes the document command on the specified document.
         *
         * @param document the document on which to execute the command.
         * @throws BadLocationException in case this commands cannot be executed
         */
        public void execute(IDocument document) throws BadLocationException {

            if (fLength == 0 && fText == null) {
                return;
            }

            if (fOwner != null) {
                document.removeDocumentListener(fOwner);
            }

            document.replace(fOffset, fLength, fText);

            if (fOwner != null) {
                document.addDocumentListener(fOwner);
            }
        }

        @Override
        public int compareTo(Command object) {
            if (isEqual(object)) {
                return 0;
            }

            Command command = object;

            // diff middle points if not intersecting
            if (fOffset + fLength <= command.fOffset || command.fOffset + command.fLength <= fOffset) {
                int value = (2 * fOffset + fLength) - (2 * command.fOffset + command.fLength);
                if (value != 0) {
                    return value;
                }
            }
            // the answer
            return 42;
        }

        private boolean isEqual(Object object) {
            if (object == this) {
                return true;
            }
            if (!(object instanceof Command)) {
                return false;
            }
            final Command command = (Command) object;
            return command.fOffset == fOffset && command.fLength == fLength;
        }
    }

    /**
     * An iterator, which iterates in reverse over a list.
     *
     * @param <E> the type of elements returned by this iterator
     */
    private static class ReverseListIterator<E> implements Iterator<E> {

        /** The list iterator. */
        private final ListIterator<E> fListIterator;

        /**
         * Creates a reverse list iterator.
         * @param listIterator the iterator that this reverse iterator is based upon
         */
        public ReverseListIterator(ListIterator<E> listIterator) {
            if (listIterator == null) {
                throw new IllegalArgumentException();
            }
            fListIterator = listIterator;
        }

        @Override
        public boolean hasNext() {
            return fListIterator.hasPrevious();
        }

        @Override
        public E next() {
            return fListIterator.previous();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * A command iterator.
     */
    private static class CommandIterator implements Iterator<Command> {

        /** The command iterator. */
        private final Iterator<Command> fIterator;

        /** The original command. */
        private Command fCommand;

        /** A flag indicating the direction of iteration. */
        private boolean fForward;

        /**
         * Creates a command iterator.
         *
         * @param commands an ascending ordered list of commands
         * @param command the original command
         * @param forward the direction
         */
        public CommandIterator(final List<Command> commands, final Command command, final boolean forward) {
            if (commands == null || command == null) {
                throw new IllegalArgumentException();
            }
            fIterator = forward ? commands.iterator()
                    : new ReverseListIterator<>(commands.listIterator(commands.size()));
            fCommand = command;
            fForward = forward;
        }

        @Override
        public boolean hasNext() {
            return fCommand != null || fIterator.hasNext();
        }

        @Override
        public Command next() {

            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            if (fCommand == null) {
                return fIterator.next();
            }

            if (!fIterator.hasNext()) {
                final Command tempCommand = fCommand;
                fCommand = null;
                return tempCommand;
            }

            final Command command = fIterator.next();
            final int compareValue = command.compareTo(fCommand);

            if ((compareValue < 0) ^ !fForward) {
                return command;

            } else if ((compareValue > 0) ^ !fForward) {
                final Command tempCommand = fCommand;
                fCommand = command;
                return tempCommand;

            } else {
                throw new IllegalArgumentException();
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /** Must the command be updated */
    public boolean doit = false;
    /** The offset of the command. */
    public int offset;
    /** The length of the command */
    public int length;
    /** The text to be inserted */
    public String text;
    /**
     * The owner of the document command which will not be notified.
     * @since 2.1
     */
    public IDocumentListener owner;
    /**
     * The caret offset with respect to the document before the document command is executed.
     * @since 2.1
     */
    public int caretOffset;
    /**
     * Additional document commands.
     * @since 2.1
     */
    private final List<Command> fCommands = new ArrayList<>();
    /**
     * Indicates whether the caret should be shifted by this command.
     * @since 3.0
     */
    public boolean shiftsCaret;

    /**
     * Creates a new document command.
     */
    protected BaseDocumentCommand() {
    }

    /**
     * Adds an additional replace command. The added replace command must not overlap
     * with existing ones. If the document command owner is not <code>null</code>, it will not
     * get document change notifications for the particular command.
     *
     * @param commandOffset the offset of the region to replace
     * @param commandLength the length of the region to replace
     * @param commandText the text to replace with, may be <code>null</code>
     * @param commandOwner the command owner, may be <code>null</code>
     * @throws BadLocationException if the added command intersects with an existing one
     * @since 2.1
     */
    public void addCommand(int commandOffset, int commandLength, String commandText, IDocumentListener commandOwner)
            throws BadLocationException {
        final Command command = new Command(commandOffset, commandLength, commandText, commandOwner);

        if (intersects(command)) {
            throw new BadLocationException();
        }

        final int index = Collections.binarySearch(fCommands, command);

        // a command with exactly the same ranges exists already
        if (index >= 0) {
            throw new BadLocationException();
        }

        // binary search result is defined as (-(insertionIndex) - 1)
        final int insertionIndex = -(index + 1);

        // overlaps to the right?
        if (insertionIndex != fCommands.size() && intersects(fCommands.get(insertionIndex), command)) {
            throw new BadLocationException();
        }

        // overlaps to the left?
        if (insertionIndex != 0 && intersects(fCommands.get(insertionIndex - 1), command)) {
            throw new BadLocationException();
        }

        fCommands.add(insertionIndex, command);
    }

    /**
     * Returns an iterator over the commands in ascending position order.
     * The iterator includes the original document command.
     * Commands cannot be removed.
     *
     * @return returns the command iterator
     */
    public Iterator<Command> getCommandIterator() {
        Command command = new Command(offset, length, text, owner);
        return new CommandIterator(fCommands, command, true);
    }

    /**
     * Returns the number of commands including the original document command.
     *
     * @return returns the number of commands
     * @since 2.1
     */
    public int getCommandCount() {
        return 1 + fCommands.size();
    }

    /**
     * Returns whether the two given commands intersect.
     *
     * @param command0 the first command
     * @param command1 the second command
     * @return <code>true</code> if the commands intersect
     * @since 2.1
     */
    private boolean intersects(Command command0, Command command1) {
        // diff middle points if not intersecting
        if (command0.fOffset + command0.fLength <= command1.fOffset
                || command1.fOffset + command1.fLength <= command0.fOffset) {
            return (2 * command0.fOffset + command0.fLength) - (2 * command1.fOffset + command1.fLength) == 0;
        }
        return true;
    }

    /**
     * Returns whether the given command intersects with this command.
     *
     * @param command the command
     * @return <code>true</code> if the command intersects with this command
     * @since 2.1
     */
    private boolean intersects(Command command) {
        // diff middle points if not intersecting
        if (offset + length <= command.fOffset || command.fOffset + command.fLength <= offset) {
            return (2 * offset + length) - (2 * command.fOffset + command.fLength) == 0;
        }
        return true;
    }

    /**
     * Returns <code>true</code> if the caret offset should be updated, <code>false</code> otherwise.
     *
     * @return <code>true</code> if the caret offset should be updated, <code>false</code> otherwise
     * @since 3.0
     */
    private boolean updateCaret() {
        return shiftsCaret && caretOffset != -1;
    }

    /**
     * Returns the position category for the caret offset position.
     *
     * @return the position category for the caret offset position
     * @since 3.0
     */
    private String getCategory() {
        return toString();
    }

    @Override
    public void setText(String string) {
        this.text = string;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public int getOffset() {
        return offset;
    }

    @Override
    public boolean getDoIt() {
        return doit;
    }

    @Override
    public int getLength() {
        return length;
    }

    @Override
    public void setShiftsCaret(boolean b) {
        this.shiftsCaret = b;
    }

    @Override
    public void setCaretOffset(int i) {
        this.caretOffset = i;
    }

    @Override
    public void setLength(int i) {
        this.length = i;
    }

    @Override
    public void setOffset(int i) {
        this.offset = i;
    }

}
