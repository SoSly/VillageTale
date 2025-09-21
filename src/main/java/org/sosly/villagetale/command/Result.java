package org.sosly.villagetale.command;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public class Result {
    private final boolean success;
    private final Component message;
    private final int returnValue;

    private Result(boolean success, Component message, int returnValue) {
        this.success = success;
        this.message = message;
        this.returnValue = returnValue;
    }

    public static Result success(Component message) {
        return new Result(true, message, 1);
    }

    public static Result failure(Component message) {
        return new Result(false, message, 0);
    }

    public static Result counted(int count, String singular, String plural) {
        if (count == 0) {
            return failure(Component.literal("No " + plural + " found"));
        }
        String message = count == 1 ? "1 " + singular : count + " " + plural;
        return new Result(true, Component.literal(message), count);
    }

    public int send(CommandSourceStack source) {
        return send(source, false);
    }

    public int send(CommandSourceStack source, boolean broadcast) {
        if (success) {
            source.sendSuccess(() -> message, broadcast);
        } else {
            source.sendFailure(message);
        }
        return returnValue;
    }

    public boolean isSuccess() {
        return success;
    }

    public int value() {
        return returnValue;
    }
}
