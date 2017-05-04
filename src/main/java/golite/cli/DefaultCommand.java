/*
 * This file is part of GoLite, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2017 Aleksi Sapon, Rohit Verma, Ayesha Krishnamurthy <https://github.com/DDoS/Golite>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package golite.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 *
 */
public class DefaultCommand extends Command {
    private static final String VERSION_OPTION = "v";

    public DefaultCommand() {
        super("");
    }

    @Override
    public String getHelp() {
        return "Commands: \"\" | \"parse\" | \"print\" | \"typecheck\" | \"irgen\" | \"codegen\" | \"compile\"\n" +
                "Use \"-h\" with any of these commands for further information";
    }

    @Override
    public void addCommandLineOptions(Options options) {
        options.addOption(new Option(VERSION_OPTION, "Print the version number"));
    }

    @Override
    public void execute(CommandLine commandLine) {
        System.out.println("Team 9 Golite compiler");
        System.out.println("v1.0.0-rc");
        if (!commandLine.hasOption(VERSION_OPTION)) {
            System.out.println(getHelp());
        }
    }

    @Override
    public void output(CommandLine commandLine) {
    }
}
