package org.elaastic.test.interpreter

import org.elaastic.activity.response.ConfidenceDegree
import org.elaastic.sequence.ExecutionContext
import org.elaastic.test.interpreter.command.*
import org.slf4j.LoggerFactory

class FunctionalTestInterpreter : Interpreter() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun parseCommand(command: String, args: List<String>) =
        when (command) {
            CommandDescriptor.START_SEQUENCE.command -> {
                require(args.size == 1) { CommandDescriptor.START_SEQUENCE.displayUsage() }
                StartSequence(
                    executionContext = ExecutionContext.valueOf(args[0])
                )
            }

            CommandDescriptor.SUBMIT_RESPONSE.command -> {
                require(args.size == 4 || args.size == 5) {
                    log.error("$args size is not valid (4 or 5) actual size is ${args.size}")
                    CommandDescriptor.SUBMIT_RESPONSE.displayUsage()
                }
                SubmitResponse(
                    phase = when (args[0]) {
                        "1" -> Phase.PHASE_1
                        "2" -> Phase.PHASE_2
                        else -> error("${args[0]} is not a valid phase")
                    },
                    username = args[1],
                    correct = args[2] == "correct",
                    confidenceDegree = ConfidenceDegree.valueOf(args[3]),
                    explanation = if (args.size == 5) args[4] else null,
                )
            }

            CommandDescriptor.EVAL.command -> {
                require(args.size == 2) { CommandDescriptor.EVAL.displayUsage() }
                Evaluate(
                    username = args[0],
                    strategy = EvaluationStrategy.valueOf(args[1]),
                )
            }

            CommandDescriptor.NEXT_PHASE.command -> NextPhase()

            CommandDescriptor.PUBLISH_RESULTS.command -> PublishResults()

            CommandDescriptor.STOP_SEQUENCE.command -> StopSequence()

            else -> error("Unknown command: $command")
        }
}