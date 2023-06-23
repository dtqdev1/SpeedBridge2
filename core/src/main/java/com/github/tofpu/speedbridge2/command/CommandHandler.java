package com.github.tofpu.speedbridge2.command;

import com.github.tofpu.speedbridge2.command.CommandExecutor.ExecutableCommand;
import com.github.tofpu.speedbridge2.command.CommandResolver.ResolvedCommand;
import com.github.tofpu.speedbridge2.command.argument.ArgumentResolver;
import com.github.tofpu.speedbridge2.command.executable.Executable;
import com.github.tofpu.speedbridge2.command.internal.MethodCommand;
import com.github.tofpu.speedbridge2.command.internal.maker.CommandMaker;
import com.github.tofpu.speedbridge2.command.internal.maker.MethodCommandMaker;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class CommandHandler {

    private final RegisteredCommandRegistry<MethodCommand> registry;
    private final CommandResolver<MethodCommand> commandResolver;
    private final CommandMaker<MethodCommand> factory;
    private final ArgumentResolver argumentResolver;
    private final CommandExecutor executor;

    public CommandHandler() {
        this.registry = new RegisteredCommandRegistry<>();
        this.commandResolver = new CommandResolver<>(registry);
        this.factory = new MethodCommandMaker();
        this.executor = new CommandExecutor();
        this.argumentResolver = new ArgumentResolver();

        registerResolvers();
    }

    protected void registerResolvers() {
        this.argumentResolver.register(Integer.class, (context) -> {
            try {
                int parseInt = Integer.parseInt(context.peek());
                context.poll();
                return parseInt;
            } catch (NumberFormatException e) {
                return null;
            }
        });

        this.argumentResolver.register(Integer[].class, (context) -> {
            final Queue<Integer> array = new LinkedList<>();

            while (context.peek() != null) {
                Integer resolve = this.argumentResolver.resolve(Integer.class, context.original());
                if (resolve == null) {
                    break;
                }
                array.add(resolve);
                context.poll();
            }
            return array.toArray(new Integer[0]);
        });

        this.argumentResolver.register(String[].class, (context -> {
            final Queue<String> array = new LinkedList<>();

            while (context.peek() != null) {
                String resolve = context.poll();
                if (resolve == null) {
                    break;
                }
                array.add(resolve);
            }
            return array.toArray(new String[0]);
        }));
    }

    public void register(Object object) {
        MethodCommand command = this.factory.create(object);
        this.registry.register(command.name(), command);
    }

    public void invoke(String command) {
        ResolvedCommand resolvedCommand = this.commandResolver.resolve(command);

        Executable executable = resolvedCommand.executable();
        Object[] arguments = argumentResolver.resolve(executable.executableParameter(),
            resolvedCommand.arguments());

        ExecutableCommand executableCommand = new ExecutableCommand(executable, arguments);
        this.executor.execute(executableCommand);
    }

    static class RegisteredCommandRegistry<T extends CommandDetail> {

        private final Map<String, T> commandMap = new HashMap<>();

        public void register(String commandName, T object) {
            this.commandMap.put(commandName, object);
        }

        public T get(String commandName) {
            return this.commandMap.get(commandName);
        }

        public boolean contains(String commandName) {
            return this.commandMap.containsKey(commandName);
        }
    }
}