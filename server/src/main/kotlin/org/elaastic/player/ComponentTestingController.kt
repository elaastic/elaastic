/*
 * Elaastic - formative assessment system
 * Copyright (C) 2019. University Toulouse 1 Capitole, University Toulouse 3 Paul Sabatier
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.elaastic.player

import org.elaastic.activity.evaluation.peergrading.draxo.DraxoEvaluation
import org.elaastic.activity.evaluation.peergrading.draxo.DraxoGrading
import org.elaastic.activity.evaluation.peergrading.draxo.criteria.Criteria
import org.elaastic.activity.evaluation.peergrading.draxo.option.OptionId
import org.elaastic.activity.response.ConfidenceDegree
import org.elaastic.activity.results.*
import org.elaastic.ai.evaluation.chatgpt.ChatGptEvaluationStatus
import org.elaastic.common.abtesting.ElaasticFeatures
import org.elaastic.common.web.MessageBuilder
import org.elaastic.material.instructional.question.ChoiceItem
import org.elaastic.material.instructional.question.ExclusiveChoiceSpecification
import org.elaastic.material.instructional.question.MultipleChoiceSpecification
import org.elaastic.material.instructional.question.QuestionType
import org.elaastic.material.instructional.question.legacy.LearnerChoice
import org.elaastic.player.chart.confidence.ConfidenceDistributionChartModel
import org.elaastic.player.chart.evaluation.EvaluationDistributionChartModel
import org.elaastic.player.chart.response.ChoiceSpecificationData
import org.elaastic.player.chart.response.ResponseDistributionChartModel
import org.elaastic.player.command.CommandModel
import org.elaastic.player.command.CommandModelFactory
import org.elaastic.player.dashboard.DashboardPhaseState
import org.elaastic.player.dashboard.LearnerMonitoringModel
import org.elaastic.player.dashboard.LearnerStateOnPhase
import org.elaastic.player.dashboard.SequenceMonitoringModel
import org.elaastic.player.evaluation.EvaluationModel
import org.elaastic.player.evaluation.chatgpt.ChatGptEvaluationModel
import org.elaastic.player.evaluation.draxo.DraxoEvaluationModel
import org.elaastic.player.explanations.*
import org.elaastic.player.recommendation.*
import org.elaastic.player.results.ChoiceResultsModel
import org.elaastic.player.results.OpenResultsModel
import org.elaastic.player.results.ResultsModel
import org.elaastic.player.results.learner.LearnerExclusiveChoiceResults
import org.elaastic.player.results.learner.LearnerMultipleChoiceResults
import org.elaastic.player.results.learner.LearnerOpenResults
import org.elaastic.player.results.learner.LearnerResultsModel
import org.elaastic.player.sequence.status.SequenceInfoModel
import org.elaastic.player.sequence.status.SequenceInfoResolver
import org.elaastic.player.statement.StatementInfoPanelModel
import org.elaastic.player.steps.SequenceStatistics
import org.elaastic.player.steps.StepsModel
import org.elaastic.sequence.ExecutionContext
import org.elaastic.sequence.Sequence
import org.elaastic.sequence.SequenceGenerator
import org.elaastic.sequence.State
import org.elaastic.sequence.config.ResponseSubmissionSpecification
import org.elaastic.activity.response.ExclusiveChoiceResponseData
import org.elaastic.activity.response.MultipleChoiceResponseData
import org.elaastic.activity.response.OpenEndedResponseData
import org.elaastic.sequence.phase.evaluation.all_at_once.AllAtOnceLearnerEvaluationPhase
import org.elaastic.sequence.phase.evaluation.all_at_once.AllAtOnceLearnerEvaluationPhaseViewModel
import org.elaastic.sequence.phase.evaluation.draxo.DraxoLearnerEvaluationPhase
import org.elaastic.sequence.phase.evaluation.draxo.DraxoLearnerEvaluationPhaseViewModel
import org.elaastic.sequence.phase.response.LearnerResponseFormViewModel
import org.elaastic.sequence.phase.response.LearnerResponsePhaseViewModel
import org.elaastic.user.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.togglz.core.Feature
import org.togglz.core.manager.FeatureManager
import java.math.BigDecimal
import kotlin.random.Random

@Controller
@RequestMapping("/player/test")
@PreAuthorize("@featureManager.isActive(@featureResolver.getFeature('FUNCTIONAL_TESTING'))")
class ComponentTestingController(
    @Autowired
    val messageBuilder: MessageBuilder,

    @Autowired
    val featureManager: FeatureManager,
    private val userService: UserService,
    private val userRepository: UserRepository

) {

    @GetMapping("/index", "/", "")
    fun index(
        authentication: Authentication,
        model: Model
    ): String {
        val user: User = authentication.principal as User
        model.addAttribute("user", user)

        return "player/test-index"
    }


    @GetMapping("/steps")
    fun testSteps(
        authentication: Authentication,
        model: Model,
        @RequestParam responseSubmissionState: StepsModel.PhaseState?,
        @RequestParam evaluationState: StepsModel.PhaseState?,
        @RequestParam readState: StepsModel.PhaseState?,
        @RequestParam showStatistics: Boolean?,
        @RequestParam studentsProvideExplanation: Boolean?
    ): String {
        val user: User = authentication.principal as User

        model.addAttribute("user", user)
        model.addAttribute(
            "stepsModel",
            StepsModel(
                responseSubmissionState = responseSubmissionState ?: StepsModel.PhaseState.COMPLETED,
                evaluationState = evaluationState ?: StepsModel.PhaseState.ACTIVE,
                readState = readState ?: StepsModel.PhaseState.DISABLED,
                showStatistics = showStatistics ?: false,
                studentsProvideExplanation = studentsProvideExplanation ?: true
            )
        )
        model.addAttribute("sequenceStatistics", SequenceStatistics(10, 8, 5))

        return "player/assignment/sequence/components/steps/test-steps"
    }

    @GetMapping("/explanation-viewer")
    fun testExplanationViewer(
        authentication: Authentication,
        model: Model
    ): String {
        val user: User = authentication.principal as User

        model.addAttribute("user", user)
        model.addAttribute(
            "explanationViewerSituationArray",
            with(ExplanationViewerCmd) {
                arrayOf(
                    choiceQuestionSituation {
                        sequenceId = 1
                        description = "1. Question à choix, cas général"
                        explanationsByResponse {
                            response(listOf(1), 100, true) {
                                explanation {
                                    nbEvaluations = 1
                                    meanGrade = BigDecimal(3)
                                    confidenceDegree = ConfidenceDegree.TOTALLY_CONFIDENT
                                    author = "Joe Walson (@Jwal)"
                                    content = "Explication B"
                                }
                                explanation {
                                    nbEvaluations = 4
                                    meanGrade = BigDecimal(5)
                                    confidenceDegree = ConfidenceDegree.NOT_CONFIDENT_AT_ALL
                                    author = "Bob Hart (@Bhar)"
                                    content = "Explication C"
                                }
                                explanation {
                                    nbEvaluations = 0
                                    author = "Arthur Rodriguez (@Arod)"
                                    confidenceDegree = ConfidenceDegree.NOT_REALLY_CONFIDENT
                                    content = "Explication D"
                                }
                            }
                            response(listOf(2), 0, false) {
                                explanation {
                                    nbEvaluations = 2
                                    meanGrade = BigDecimal("1.33")
                                    author = "Bill Gates (@Bgat)"
                                    confidenceDegree = ConfidenceDegree.CONFIDENT
                                    content = "Explication A"
                                }
                            }
                        }
                    },
                    choiceQuestionSituation {
                        sequenceId = 2
                        description = "2. Aucune explication fournie"
                        explanationsByResponse {
                            response(listOf(1), 100, true) {}
                            response(listOf(2), 0, false) {}
                        }
                    },
                    choiceQuestionSituation {
                        sequenceId = 3
                        description = "3. Uniquement 2 explications correctes"
                        explanationsByResponse {
                            response(listOf(1), 100, true) {
                                explanation {
                                    nbEvaluations = 1
                                    meanGrade = BigDecimal(3)
                                    author = "Joe Walson (@Jwal)"
                                    confidenceDegree = ConfidenceDegree.CONFIDENT
                                    content = "Explication B"
                                }
                                explanation {
                                    nbEvaluations = 4
                                    meanGrade = BigDecimal(5)
                                    author = "Bob Hart (@Bhar)"
                                    confidenceDegree = ConfidenceDegree.TOTALLY_CONFIDENT
                                    content = "Explication C"
                                }
                            }
                        }
                    },
                    choiceQuestionSituation {
                        sequenceId = 4
                        description = "4. Seulement des explications incorrectes"
                        explanationsByResponse {
                            response(listOf(1), 100, true) {}
                            response(listOf(2), 0, false) {
                                explanation {
                                    nbEvaluations = 1
                                    meanGrade = BigDecimal(3)
                                    author = "Joe Walson (@Jwal)"
                                    confidenceDegree = ConfidenceDegree.NOT_REALLY_CONFIDENT
                                    content = "Explication B"
                                }
                                explanation {
                                    nbEvaluations = 4
                                    meanGrade = BigDecimal(5)
                                    author = "Bob Hart (@Bhar)"
                                    confidenceDegree = ConfidenceDegree.NOT_CONFIDENT_AT_ALL
                                    content = "Explication C"
                                }
                            }
                        }
                    },
                    openQuestionSituation {
                        sequenceId = 5
                        description = "5. Plusieurs explications pour une question ouverte"
                        explanations {
                            explanation {
                                nbEvaluations = 2
                                meanGrade = BigDecimal("1.33")
                                author = "Bill Gates (@Bgat)"
                                confidenceDegree = ConfidenceDegree.CONFIDENT
                                content = "Explication A"
                            }
                            explanation {
                                nbEvaluations = 1
                                meanGrade = BigDecimal(3)
                                author = "Joe Walson (@Jwal)"
                                confidenceDegree = ConfidenceDegree.TOTALLY_CONFIDENT
                                content = "Explication B"
                            }
                            explanation {
                                nbEvaluations = 4
                                meanGrade = BigDecimal(5)
                                author = "Bob Hart (@Bhar)"
                                confidenceDegree = ConfidenceDegree.CONFIDENT
                                content = "Explication C"
                            }
                            explanation {
                                nbEvaluations = 0
                                author = "Arthur Rodriguez (@Arod)"
                                confidenceDegree = ConfidenceDegree.TOTALLY_CONFIDENT
                                content = "Explication D"
                            }
                        }
                    },
                    choiceQuestionSituation {
                        sequenceId = 6
                        description = "6. Question à choix multiples"
                        explanationsByResponse {
                            response(listOf(1, 3), 100, true) {
                                explanation {
                                    nbEvaluations = 1
                                    meanGrade = BigDecimal(3)
                                    author = "Joe Walson (@Jwal)"
                                    confidenceDegree = ConfidenceDegree.NOT_CONFIDENT_AT_ALL
                                    content = "Explication B"
                                }
                                explanation {
                                    nbEvaluations = 4
                                    meanGrade = BigDecimal(5)
                                    author = "Bob Hart (@Bhar)"
                                    confidenceDegree = ConfidenceDegree.NOT_CONFIDENT_AT_ALL
                                    content = "Explication C"
                                }
                                explanation {
                                    nbEvaluations = 0
                                    author = "Arthur Rodriguez (@Arod)"
                                    confidenceDegree = ConfidenceDegree.NOT_CONFIDENT_AT_ALL
                                    content = "Explication D"
                                }
                            }
                            response(listOf(1, 2), 30, false) {
                                explanation {
                                    nbEvaluations = 2
                                    meanGrade = BigDecimal("1.33")
                                    author = "Bill Gates (@Bgat)"
                                    confidenceDegree = ConfidenceDegree.NOT_REALLY_CONFIDENT
                                    content = "Explication A"
                                }
                            }
                            response(listOf(3), 0, false) {
                                explanation {
                                    nbEvaluations = 2
                                    meanGrade = BigDecimal("1.33")
                                    author = "Bill Gates (@Bgat)"
                                    confidenceDegree = ConfidenceDegree.NOT_CONFIDENT_AT_ALL
                                    content = "Explication E"
                                }
                                explanation {
                                    nbEvaluations = 0
                                    meanGrade = null
                                    author = "Bill Gates (@Bgat)"
                                    confidenceDegree = ConfidenceDegree.TOTALLY_CONFIDENT
                                    content = "Explication F"
                                }
                                explanation {
                                    nbEvaluations = 1
                                    meanGrade = BigDecimal(5)
                                    author = "Bill Gates (@Bgat)"
                                    confidenceDegree = ConfidenceDegree.CONFIDENT
                                    content = "Explication G"
                                }
                                explanation {
                                    nbEvaluations = 4
                                    meanGrade = BigDecimal(2)
                                    confidenceDegree = ConfidenceDegree.NOT_CONFIDENT_AT_ALL
                                    author = "Bill Gates (@Bgat)"
                                    content = "Explication H"
                                }
                            }
                        }
                    },
                    choiceQuestionSituation {
                        sequenceId = 7
                        description = "7. Uniquement l'explication de l'enseignant, pour une question a choix multiples"
                        explanationsByResponse {
                            response(listOf(1), 100, true) {
                                explanation {
                                    author = "Franck Sil (@fsil)"
                                    confidenceDegree = ConfidenceDegree.CONFIDENT
                                    content = "Explication de l'enseignant"
                                    fromTeacher = true
                                }
                            }
                        }
                    },
                    choiceQuestionSituation {
                        sequenceId = 8
                        description =
                            "8. Plusieurs explications et explication de l'enseignant, pour une question a choix multiples"
                        explanationsByResponse {
                            response(listOf(1, 3), 100, true) {
                                explanation {
                                    nbEvaluations = 3
                                    meanGrade = BigDecimal("3.25")
                                    author = "Franck Sil (@fsil)"
                                    confidenceDegree = ConfidenceDegree.CONFIDENT
                                    content = "Explication de l'enseignant"
                                    fromTeacher = true
                                }
                                explanation {
                                    nbEvaluations = 1
                                    meanGrade = BigDecimal(3)
                                    author = "Joe Walson (@Jwal)"
                                    confidenceDegree = ConfidenceDegree.NOT_CONFIDENT_AT_ALL
                                    content = "Explication B"
                                }
                                explanation {
                                    nbEvaluations = 4
                                    meanGrade = BigDecimal(5)
                                    author = "Bob Hart (@Bhar)"
                                    confidenceDegree = ConfidenceDegree.NOT_CONFIDENT_AT_ALL
                                    content = "Explication C"
                                }
                                explanation {
                                    nbEvaluations = 0
                                    author = "Arthur Rodriguez (@Arod)"
                                    confidenceDegree = ConfidenceDegree.NOT_CONFIDENT_AT_ALL
                                    content = "Explication D"
                                }
                            }
                            response(listOf(1, 2), 30, false) {
                                explanation {
                                    nbEvaluations = 2
                                    meanGrade = BigDecimal("1.33")
                                    author = "Bill Gates (@Bgat)"
                                    confidenceDegree = ConfidenceDegree.NOT_REALLY_CONFIDENT
                                    content = "Explication A"
                                }
                            }
                            response(listOf(3), 0, false) {
                                explanation {
                                    nbEvaluations = 2
                                    meanGrade = BigDecimal("1.33")
                                    author = "Bill Gates (@Bgat)"
                                    confidenceDegree = ConfidenceDegree.NOT_CONFIDENT_AT_ALL
                                    content = "Explication E"
                                }
                                explanation {
                                    nbEvaluations = 0
                                    meanGrade = null
                                    author = "Bill Gates (@Bgat)"
                                    confidenceDegree = ConfidenceDegree.TOTALLY_CONFIDENT
                                    content = "Explication F"
                                }
                                explanation {
                                    nbEvaluations = 1
                                    meanGrade = BigDecimal(5)
                                    author = "Bill Gates (@Bgat)"
                                    confidenceDegree = ConfidenceDegree.CONFIDENT
                                    content = "Explication G"
                                }
                                explanation {
                                    nbEvaluations = 4
                                    meanGrade = BigDecimal(2)
                                    confidenceDegree = ConfidenceDegree.NOT_CONFIDENT_AT_ALL
                                    author = "Bill Gates (@Bgat)"
                                    content = "Explication H"
                                }
                            }
                        }
                    },
                )
            }
        )

        return "player/assignment/sequence/components/explanation-viewer/test-explanation-viewer"
    }

    class ExplanationViewerSituation(
        val description: String,
        val sequenceId: Long,
        val explanationViewerModel: ExplanationViewerModel
    )

    @GetMapping("/response-distribution-chart")
    fun testResponseDistributionChat(
        authentication: Authentication,
        model: Model
    ): String {
        val user: User = authentication.principal as User

        model.addAttribute("user", user)
        model.addAttribute(
            "responseDistributionChartSituations",
            listOf(
                ResponseDistributionChartSituation(
                    description = "1 seule tentative, 2 items, pas de sans réponse",
                    model = ResponseDistributionChartModel(
                        interactionId = 12,
                        choiceSpecification = ChoiceSpecificationData(
                            2,
                            listOf(1)
                        ),
                        results = ResponsesDistribution(
                            ResponsesDistributionOnAttempt(10, arrayOf(7, 3), 0)
                        ).toLegacyFormat()
                    )

                ),
                ResponseDistributionChartSituation(
                    description = "1 seule tentative, 4 items, avec sans réponse",
                    model = ResponseDistributionChartModel(
                        interactionId = 14,
                        choiceSpecification = ChoiceSpecificationData(
                            4,
                            listOf(3)
                        ),
                        results = ResponsesDistribution(
                            ResponsesDistributionOnAttempt(10, arrayOf(1, 0, 5, 2), 2)
                        ).toLegacyFormat()
                    )

                ),
                ResponseDistributionChartSituation(
                    description = "1 seule tentative, 10 items, avec sans réponse",
                    model = ResponseDistributionChartModel(
                        interactionId = 110,
                        choiceSpecification = ChoiceSpecificationData(
                            10,
                            listOf(7)
                        ),
                        results = ResponsesDistribution(
                            ResponsesDistributionOnAttempt(60, arrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
                        ).toLegacyFormat()
                    )

                ),
                ResponseDistributionChartSituation(
                    description = "2 tentatives, 2 items, pas de sans réponse",
                    model = ResponseDistributionChartModel(
                        interactionId = 22,
                        choiceSpecification = ChoiceSpecificationData(
                            2,
                            listOf(1)
                        ),
                        results = ResponsesDistribution(
                            ResponsesDistributionOnAttempt(10, arrayOf(7, 3)),
                            ResponsesDistributionOnAttempt(10, arrayOf(9, 1))
                        ).toLegacyFormat()
                    )

                ),
                ResponseDistributionChartSituation(
                    description = "2 tentatives, 4 items, avec sans réponse à la 1ère tentative",
                    model = ResponseDistributionChartModel(
                        interactionId = 241,
                        choiceSpecification = ChoiceSpecificationData(
                            4,
                            listOf(3)
                        ),
                        results = ResponsesDistribution(
                            ResponsesDistributionOnAttempt(10, arrayOf(1, 0, 5, 2), 2),
                            ResponsesDistributionOnAttempt(10, arrayOf(1, 2, 1, 0), 0)
                        ).toLegacyFormat()
                    )

                ),
                ResponseDistributionChartSituation(
                    description = "2 tentatives, 4 items, avec sans réponse à la 2ème tentative",
                    model = ResponseDistributionChartModel(
                        interactionId = 242,
                        choiceSpecification = ChoiceSpecificationData(
                            4,
                            listOf(3)
                        ),
                        results = ResponsesDistribution(
                            ResponsesDistributionOnAttempt(10, arrayOf(1, 2, 5, 2), 0),
                            ResponsesDistributionOnAttempt(10, arrayOf(1, 0, 1, 0), 2)
                        ).toLegacyFormat()
                    )

                ),
                ResponseDistributionChartSituation(
                    description = "2 tentatives, 4 items, avec sans réponse aux 2 tentatives",
                    model = ResponseDistributionChartModel(
                        interactionId = 243,
                        choiceSpecification = ChoiceSpecificationData(
                            4,
                            listOf(3)
                        ),
                        results = ResponsesDistribution(
                            ResponsesDistributionOnAttempt(10, arrayOf(1, 1, 5, 1), 2),
                            ResponsesDistributionOnAttempt(10, arrayOf(1, 0, 1, 0), 2)
                        ).toLegacyFormat()
                    )

                ),
                ResponseDistributionChartSituation(
                    description = "2 tentatives, 10 items, avec sans réponse",
                    model = ResponseDistributionChartModel(
                        interactionId = 210,
                        choiceSpecification = ChoiceSpecificationData(
                            10,
                            listOf(7)
                        ),
                        results = ResponsesDistribution(
                            ResponsesDistributionOnAttempt(60, arrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), 5),
                            ResponsesDistributionOnAttempt(60, arrayOf(10, 9, 8, 7, 6, 5, 4, 3, 2, 1), 5)
                        ).toLegacyFormat()
                    )

                ),
                ResponseDistributionChartSituation(
                    description = "Choix multiples",
                    model = ResponseDistributionChartModel(
                        interactionId = 999,
                        choiceSpecification = ChoiceSpecificationData(
                            4,
                            listOf(1, 3)
                        ),
                        results = ResponsesDistribution(
                            ResponsesDistributionOnAttempt(4, arrayOf(2, 1, 1, 1), 2),
                            ResponsesDistributionOnAttempt(4, arrayOf(2, 1, 3, 1), 0)
                        ).toLegacyFormat()
                    )

                )
            )

        )

        return "player/assignment/sequence/components/response-distribution-chart/test-response-distribution-chart"
    }

    data class ResponseDistributionChartSituation(
        val description: String,
        val model: ResponseDistributionChartModel
    )

    @GetMapping("/statement")
    fun testStatement(
        authentication: Authentication,
        model: Model,
        @RequestParam panelClosed: Boolean?,
        @RequestParam hideQuestionType: Boolean?,
        @RequestParam hideStatement: Boolean?
    ): String {
        val user: User = authentication.principal as User

        model.addAttribute("user", user)
        model.addAttribute(
            "statementInfoPanelModel",
            StatementInfoPanelModel(
                title = "Énoncé de test",
                questionType = QuestionType.ExclusiveChoice,
                content = "Le <strong>contenu</strong> de cet énoncé de test.",
                panelClosed = panelClosed ?: false,
                hideQuestionType = hideQuestionType ?: false,
                hideStatement = hideStatement ?: false
            )
        )

        return "player/assignment/sequence/components/statement/test-statement"
    }

    @GetMapping("/my-results")
    fun testMyResults(
        authentication: Authentication,
        model: Model
    ): String {
        val user: User = authentication.principal as User
        model.addAttribute("user", user)

        model.addAttribute(
            "myResultsSituations",
            listOf(
                MyResultsSituation(
                    description = "Choix exclusif, incorrect puis correct, avec explications",
                    learnerResultsModel = LearnerExclusiveChoiceResults(
                        explanationFirstTry = ExplanationData(responseId = Random.nextLong(), content = "I was wrong"),
                        explanationSecondTry = ExplanationData(
                            responseId = Random.nextLong(),
                            content = "And I've changed my mind"
                        ),
                        choiceFirstTry = LearnerChoice(listOf(2)),
                        choiceSecondTry = LearnerChoice(listOf(1)),
                        scoreFirstTry = 0,
                        scoreSecondTry = 100,
                        expectedChoice = ExclusiveChoiceSpecification(
                            nbCandidateItem = 4,
                            expectedChoice = ChoiceItem(1, 1.0f)
                        )
                    )
                ),
                MyResultsSituation(
                    description = "Choix exclusif, incorrect puis correct, sans explications",
                    learnerResultsModel = LearnerExclusiveChoiceResults(
                        explanationFirstTry = null,
                        explanationSecondTry = null,
                        choiceFirstTry = LearnerChoice(listOf(2)),
                        choiceSecondTry = LearnerChoice(listOf(1)),
                        scoreFirstTry = 0,
                        scoreSecondTry = 100,
                        expectedChoice = ExclusiveChoiceSpecification(
                            nbCandidateItem = 4,
                            expectedChoice = ChoiceItem(1, 1.0f)
                        )
                    )
                ),
                MyResultsSituation(
                    description = "Choix exclusif, réponses identiques, sans explications",
                    learnerResultsModel = LearnerExclusiveChoiceResults(
                        explanationFirstTry = null,
                        explanationSecondTry = null,
                        choiceFirstTry = LearnerChoice(listOf(2)),
                        choiceSecondTry = LearnerChoice(listOf(2)),
                        scoreFirstTry = 0,
                        scoreSecondTry = 0,
                        expectedChoice = ExclusiveChoiceSpecification(
                            nbCandidateItem = 4,
                            expectedChoice = ChoiceItem(1, 1.0f)
                        )
                    )
                ),
                MyResultsSituation(
                    description = "Choix multiple, résultats qui s'améliorent, avec explications",
                    learnerResultsModel = LearnerMultipleChoiceResults(
                        explanationFirstTry = ExplanationData(responseId = Random.nextLong(), content = "so-so"),
                        explanationSecondTry = ExplanationData(
                            responseId = Random.nextLong(),
                            content = "a bit better"
                        ),
                        choiceFirstTry = LearnerChoice(listOf(2, 4)),
                        choiceSecondTry = LearnerChoice(listOf(2, 3, 4)),
                        scoreFirstTry = 0,
                        scoreSecondTry = 50,
                        expectedChoice = MultipleChoiceSpecification(
                            nbCandidateItem = 4,
                            expectedChoiceList = listOf(
                                ChoiceItem(2, 0.5f),
                                ChoiceItem(3, 0.5f)
                            )
                        )
                    )
                ),
                MyResultsSituation(
                    description = "Question ouverte - 2 explanations",
                    learnerResultsModel = LearnerOpenResults(
                        explanationFirstTry = ExplanationData(responseId = Random.nextLong(), content = "1st guess"),
                        explanationSecondTry = ExplanationData(responseId = Random.nextLong(), content = "2nd guess")
                    )
                ),
                MyResultsSituation(
                    description = "Question ouverte - 1 explanation only",
                    learnerResultsModel = LearnerOpenResults(
                        explanationFirstTry = ExplanationData(responseId = Random.nextLong(), content = "Only one"),
                        explanationSecondTry = null
                    )
                ),
                MyResultsSituation(
                    description = "Question ouverte - 2 identical explanations",
                    learnerResultsModel = LearnerOpenResults(
                        explanationFirstTry = ExplanationData(
                            responseId = Random.nextLong(),
                            content = "same explanation"
                        ),
                        explanationSecondTry = ExplanationData(
                            responseId = Random.nextLong(),
                            content = "same explanation"
                        )
                    )
                ),
                MyResultsSituation(
                    description = "Question ouverte - 2 identical explanations, second graded",
                    learnerResultsModel = LearnerOpenResults(
                        explanationFirstTry = ExplanationData(
                            responseId = Random.nextLong(),
                            content = "same explanation"
                        ),
                        explanationSecondTry = ExplanationData(
                            responseId = 456L,
                            content = "same explanation",
                            nbEvaluations = 2,
                            meanGrade = BigDecimal("3.5")
                        )
                    )
                ),
                MyResultsSituation(
                    description = "Question ouverte - only the second explanation",
                    learnerResultsModel = LearnerOpenResults(
                        explanationFirstTry = null,
                        explanationSecondTry = ExplanationData(responseId = Random.nextLong(), content = "Just the 2nd")
                    )
                ),
                MyResultsSituation(
                    description = "Question ouverte - no explanations",
                    learnerResultsModel = LearnerOpenResults(
                        explanationFirstTry = null,
                        explanationSecondTry = null
                    )
                )

            )
        )

        return "player/assignment/sequence/components/my-results/test-my-results"
    }

    data class MyResultsSituation(
        val description: String,
        val learnerResultsModel: LearnerResultsModel
    )

    @GetMapping("/results")
    fun testResults(
        authentication: Authentication,
        model: Model
    ): String {
        val user: User = authentication.principal as User

        model.addAttribute("user", user)

        model.addAttribute(
            "resultsSituations",
            listOf(
                ResultsSituation(
                    description = "1. Sequence running, hasChoices, no results, no explanations",
                    resultsModel = ChoiceResultsModel(
                        sequenceIsStopped = false,
                        sequenceId = 1
                    )
                ),
                ResultsSituation(
                    description = "2. Sequence stopped, hasChoices, no results, no explanations",
                    resultsModel = ChoiceResultsModel(
                        sequenceIsStopped = true,
                        sequenceId = 2
                    )
                ),
                ResultsSituation(
                    description = "3. Sequence running, Open question, no results, no explanations",
                    resultsModel = ChoiceResultsModel(
                        sequenceIsStopped = false,
                        sequenceId = 3
                    )
                ),
                ResultsSituation(
                    description = "4. Sequence stopped, Open question, no results, no explanations",
                    resultsModel = OpenResultsModel(
                        sequenceIsStopped = true,
                        sequenceId = 4
                    )
                ),
                ResultsSituation(
                    description = "5. Sequence running, hasChoices, has results, no explanations",
                    resultsModel = ChoiceResultsModel(
                        sequenceIsStopped = false,
                        sequenceId = 5,
                        responseDistributionChartModel = ResponseDistributionChartModel(
                            interactionId = 5,
                            choiceSpecification = ChoiceSpecificationData(
                                2,
                                listOf(2)
                            ),
                            results = ResponsesDistribution(
                                ResponsesDistributionOnAttempt(4, arrayOf(1, 3), 0)
                            ).toLegacyFormat()
                        ),
                        confidenceDistributionChartModel = ConfidenceDistributionChartModel(
                            interactionId = 5,
                            choiceSpecification = ChoiceSpecificationData(
                                2,
                                listOf(2)
                            ),
                            results = ConfidenceDistribution(
                                listOf(
                                    ConfidenceDistributionOnResponse(1, arrayOf(1, 0, 0, 0), 0),
                                    ConfidenceDistributionOnResponse(3, arrayOf(1, 0, 0, 2), 0)
                                )
                            ).toJSON()
                        )
                    )
                ),
                ResultsSituation(
                    description = "6. Sequence running, hasChoices, has results, has explanations",
                    resultsModel = ChoiceResultsModel(
                        sequenceIsStopped = false,
                        sequenceId = 6,
                        responseDistributionChartModel = ResponseDistributionChartModel(
                            interactionId = 6,
                            choiceSpecification = ChoiceSpecificationData(
                                2,
                                listOf(2)
                            ),
                            results = ResponsesDistribution(
                                ResponsesDistributionOnAttempt(4, arrayOf(1, 3), 0)
                            ).toLegacyFormat()
                        ),
                        confidenceDistributionChartModel = ConfidenceDistributionChartModel(
                            interactionId = 6,
                            choiceSpecification = ChoiceSpecificationData(
                                2,
                                listOf(2)
                            ),
                            results = ConfidenceDistribution(
                                listOf(
                                    ConfidenceDistributionOnResponse(1, arrayOf(0, 1, 0, 0), 0),
                                    ConfidenceDistributionOnResponse(3, arrayOf(1, 1, 1, 0), 0)
                                )
                            ).toJSON()
                        ),
                        explanationViewerModel = ChoiceExplanationViewerModel(
                            explanationsByResponse = mapOf(
                                ResponseData(
                                    listOf(1),
                                    0,
                                    false
                                )
                                        to listOf(
                                    ExplanationData(
                                        responseId = Random.nextLong(),
                                        "explication 1",
                                        "Joe Walson (@Jwal)",
                                        confidenceDegree = ConfidenceDegree.NOT_REALLY_CONFIDENT,
                                        score = BigDecimal("0")

                                    ),
                                    ExplanationData(
                                        responseId = Random.nextLong(),
                                        "explication 2",
                                        "Jack DiCaprio (@Jdic)",
                                        confidenceDegree = ConfidenceDegree.CONFIDENT,
                                        score = BigDecimal("0")
                                    ),
                                    ExplanationData(
                                        responseId = Random.nextLong(),
                                        "explication 3",
                                        "Jane Doe (@Jdoe)",
                                        confidenceDegree = ConfidenceDegree.TOTALLY_CONFIDENT,
                                        score = BigDecimal("0")
                                    )
                                ),
                                ResponseData(
                                    listOf(2),
                                    100,
                                    true
                                )
                                        to listOf(
                                    ExplanationData(
                                        responseId = Random.nextLong(),
                                        "explication 4",
                                        "Wiliam Shakespeare (@Wsha)",
                                        confidenceDegree = ConfidenceDegree.CONFIDENT,
                                        score = BigDecimal("100")
                                    ),
                                    ExplanationData(
                                        responseId = Random.nextLong(),
                                        "explication 5",
                                        "Averell Collignon (@Acol)",
                                        confidenceDegree = ConfidenceDegree.NOT_CONFIDENT_AT_ALL,
                                        score = BigDecimal("100")
                                    )
                                )
                            )
                        )
                    )
                ),
                ResultsSituation(
                    description = "7. Sequence running, Open question, has explanations",
                    resultsModel = OpenResultsModel(
                        sequenceIsStopped = false,
                        sequenceId = 7,
                        explanationViewerModel = OpenExplanationViewerModel(
                            explanations = listOf(
                                ExplanationData(
                                    responseId = Random.nextLong(),
                                    "explication 1",
                                    "Joe Walson (@Jwal)"
                                ),
                                ExplanationData(
                                    responseId = Random.nextLong(),
                                    "explication 2",
                                    "Jack DiCaprio (@Jdic)"
                                ),

                                ExplanationData(
                                    responseId = Random.nextLong(),
                                    "explication 3",
                                    "Wiliam Shakespeare (@Wsha)"
                                ),
                                ExplanationData(
                                    responseId = Random.nextLong(),
                                    "explication 4",
                                    "Averell Collignon (@Acol)"
                                )
                            )
                        )
                    )
                ),
                ResultsSituation(
                    description = "8. Sequence running, hasChoices, has results, has explanations, has evaluations, ppeer > 0",
                    resultsModel = ChoiceResultsModel(
                        sequenceIsStopped = false,
                        sequenceId = 8,
                        responseDistributionChartModel = ResponseDistributionChartModel(
                            interactionId = 8,
                            choiceSpecification = ChoiceSpecificationData(
                                2,
                                listOf(2)
                            ),
                            results = ResponsesDistribution(
                                ResponsesDistributionOnAttempt(4, arrayOf(1, 3), 0)
                            ).toLegacyFormat()
                        ),
                        confidenceDistributionChartModel = ConfidenceDistributionChartModel(
                            interactionId = 8,
                            choiceSpecification = ChoiceSpecificationData(
                                2,
                                listOf(2)
                            ),
                            results = ConfidenceDistribution(
                                listOf(
                                    ConfidenceDistributionOnResponse(3, arrayOf(0, 1, 1, 1), 0),
                                    ConfidenceDistributionOnResponse(3, arrayOf(1, 1, 1, 0), 0)
                                )
                            ).toJSON()
                        ),
                        explanationViewerModel = ChoiceExplanationViewerModel(
                            explanationsByResponse = mapOf(
                                ResponseData(
                                    listOf(1),
                                    0,
                                    false
                                )
                                        to listOf(
                                    ExplanationData(
                                        responseId = Random.nextLong(),
                                        "explication 1 With Draxo eval",
                                        "Joe Walson (@Jwal)",
                                        4,
                                        4,
                                        BigDecimal("3.5"),
                                        score = BigDecimal("0"),
                                        choiceList = LearnerChoice(listOf(1))
                                    ),
                                    ExplanationData(
                                        responseId = Random.nextLong(),
                                        "explication 2 With Draxo eval",
                                        "Jack DiCaprio (@Jdic)",
                                        2,
                                        2,
                                        BigDecimal("1.5"),
                                        confidenceDegree = ConfidenceDegree.NOT_CONFIDENT_AT_ALL,
                                        score = BigDecimal("0"),
                                        choiceList = LearnerChoice(listOf(1))
                                    )
                                ),
                                ResponseData(
                                    listOf(2),
                                    100,
                                    true
                                )
                                        to listOf(
                                    ExplanationData(
                                        responseId = Random.nextLong(),
                                        "explication 3 without Draxo Eval",
                                        "Wiliam Shakespeare (@Wsha)",
                                        1,
                                        0,
                                        BigDecimal("5"),
                                        confidenceDegree = ConfidenceDegree.NOT_REALLY_CONFIDENT,
                                        score = BigDecimal("100"),
                                        choiceList = LearnerChoice(listOf(2))
                                    ),
                                    ExplanationData(
                                        responseId = Random.nextLong(),
                                        "explication 4 without Draxo Eval",
                                        "Averell Collignon (@Acol)",
                                        3,
                                        0,
                                        BigDecimal("1"),
                                        confidenceDegree = ConfidenceDegree.TOTALLY_CONFIDENT,
                                        score = BigDecimal("100"),
                                        choiceList = LearnerChoice(listOf(2))
                                    )
                                )
                            ),
                            recommendedExplanationsComparator = CorrectAndMeanGradeComparator()
                        )
                    )
                ),
                ResultsSituation(
                    description = "9. Sequence running, hasChoices, has results, has explanations, has evaluations, ppeer < 0",
                    resultsModel = ChoiceResultsModel(
                        sequenceIsStopped = false,
                        sequenceId = 9,
                        responseDistributionChartModel = ResponseDistributionChartModel(
                            interactionId = 9,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(2)
                            ),
                            results = ResponsesDistribution(
                                ResponsesDistributionOnAttempt(10, arrayOf(1, 3, 4, 2), 0)
                            ).toLegacyFormat()
                        ),
                        confidenceDistributionChartModel = ConfidenceDistributionChartModel(
                            interactionId = 9,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(2)
                            ),
                            results = ConfidenceDistribution(
                                listOf(
                                    ConfidenceDistributionOnResponse(1, arrayOf(0, 1, 0, 0), 0),
                                    ConfidenceDistributionOnResponse(3, arrayOf(0, 2, 1, 0), 0),
                                    ConfidenceDistributionOnResponse(4, arrayOf(3, 1, 0, 0), 0),
                                    ConfidenceDistributionOnResponse(2, arrayOf(1, 0, 1, 0), 0)
                                )
                            ).toJSON()
                        ),
                        evaluationDistributionChartModel = EvaluationDistributionChartModel(
                            interactionId = 9,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(2)
                            ),
                            results = GradingDistribution(
                                listOf(
                                    GradingDistributionOnResponse(15, arrayOf(1, 7, 1, 1, 5), 0),
                                    GradingDistributionOnResponse(12, arrayOf(1, 0, 2, 8, 1), 0),
                                    GradingDistributionOnResponse(10, arrayOf(3, 0, 1, 4, 2), 0),
                                    GradingDistributionOnResponse(3, arrayOf(1, 0, 1, 1, 0), 0)
                                )
                            ).toLegacyFormat()
                        ),
                        explanationViewerModel = ChoiceExplanationViewerModel(
                            explanationsByResponse = mapOf(
                                ResponseData(
                                    listOf(1),
                                    0,
                                    false
                                )
                                        to listOf(
                                    ExplanationData(
                                        responseId = Random.nextLong(),
                                        "explication 1",
                                        "Joe Walson (@Jwal)",
                                        4,
                                        4,
                                        BigDecimal("3.5"),
                                        confidenceDegree = ConfidenceDegree.CONFIDENT,
                                        score = BigDecimal("0"),
                                        choiceList = LearnerChoice(listOf(1))
                                    ),
                                    ExplanationData(
                                        responseId = Random.nextLong(),
                                        "explication 2",
                                        "Jack DiCaprio (@Jdic)",
                                        2,
                                        2,
                                        BigDecimal("1.5"),
                                        confidenceDegree = ConfidenceDegree.NOT_CONFIDENT_AT_ALL,
                                        score = BigDecimal("0"),
                                        choiceList = LearnerChoice(listOf(1))
                                    )
                                ),
                                ResponseData(
                                    listOf(2),
                                    100,
                                    true
                                )
                                        to listOf(
                                    ExplanationData(
                                        responseId = Random.nextLong(),
                                        "explication 3",
                                        "Wiliam Shakespeare (@Wsha)",
                                        1,
                                        1,
                                        BigDecimal("5"),
                                        confidenceDegree = ConfidenceDegree.NOT_REALLY_CONFIDENT,
                                        score = BigDecimal("100"),
                                        choiceList = LearnerChoice(listOf(2))
                                    ),
                                    ExplanationData(
                                        responseId = Random.nextLong(),
                                        "explication 4",
                                        "Averell Collignon (@Acol)",
                                        3,
                                        3,
                                        BigDecimal("1"),
                                        confidenceDegree = ConfidenceDegree.TOTALLY_CONFIDENT,
                                        score = BigDecimal("100"),
                                        choiceList = LearnerChoice(listOf(2))
                                    )
                                )
                            ),
                            recommendedExplanationsComparator = IncorrectAndMeanGradeComparator()
                        )
                    )
                ),
                ResultsSituation(
                    description = "10. Sequence running, hasChoices, has results, has explanations, no evaluations, pconf > 0",
                    resultsModel = ChoiceResultsModel(
                        sequenceIsStopped = false,
                        sequenceId = 10,
                        responseDistributionChartModel = ResponseDistributionChartModel(
                            interactionId = 10,
                            choiceSpecification = ChoiceSpecificationData(
                                2,
                                listOf(2)
                            ),
                            results = ResponsesDistribution(
                                ResponsesDistributionOnAttempt(4, arrayOf(1, 3), 0)
                            ).toLegacyFormat()
                        ),
                        confidenceDistributionChartModel = ConfidenceDistributionChartModel(
                            interactionId = 10,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(2)
                            ),
                            results = ConfidenceDistribution(
                                listOf(
                                    ConfidenceDistributionOnResponse(10, arrayOf(1, 3, 1, 5), 0),
                                    ConfidenceDistributionOnResponse(17, arrayOf(2, 3, 2, 10), 0),
                                    ConfidenceDistributionOnResponse(7, arrayOf(1, 1, 2, 4), 0),
                                    ConfidenceDistributionOnResponse(10, arrayOf(3, 1, 5, 1), 0)
                                )
                            ).toJSON()
                        ),
                        explanationViewerModel = ChoiceExplanationViewerModel(
                            explanationsByResponse = mapOf(
                                ResponseData(
                                    listOf(1),
                                    0,
                                    false
                                )
                                        to listOf(
                                    ExplanationData(
                                        responseId = Random.nextLong(),
                                        "explication 1",
                                        "Joe Walson (@Jwal)",
                                        confidenceDegree = ConfidenceDegree.CONFIDENT,
                                        score = BigDecimal("0"),
                                        choiceList = LearnerChoice(listOf(1))
                                    ),
                                    ExplanationData(
                                        responseId = Random.nextLong(),
                                        "explication 2",
                                        "Jack DiCaprio (@Jdic)",
                                        confidenceDegree = ConfidenceDegree.NOT_CONFIDENT_AT_ALL,
                                        score = BigDecimal("0"),
                                        choiceList = LearnerChoice(listOf(1))
                                    )
                                ),
                                ResponseData(
                                    listOf(2),
                                    100,
                                    true
                                )
                                        to listOf(
                                    ExplanationData(
                                        responseId = Random.nextLong(),
                                        "explication 3",
                                        "Wiliam Shakespeare (@Wsha)",
                                        confidenceDegree = ConfidenceDegree.NOT_REALLY_CONFIDENT,
                                        score = BigDecimal("100"),
                                        choiceList = LearnerChoice(listOf(2))
                                    ),
                                    ExplanationData(
                                        responseId = Random.nextLong(),
                                        "explication 4",
                                        "Averell Collignon (@Acol)",
                                        confidenceDegree = ConfidenceDegree.TOTALLY_CONFIDENT,
                                        score = BigDecimal("100"),
                                        choiceList = LearnerChoice(listOf(2))
                                    )
                                )
                            ),
                            recommendedExplanationsComparator = CorrectAndConfidenceDegreeComparator()
                        )
                    )
                ),
                ResultsSituation(
                    description = "11. Sequence running, hasChoices, has results, has explanations, no evaluations, pconf < 0",
                    resultsModel = ChoiceResultsModel(
                        sequenceIsStopped = false,
                        sequenceId = 11,
                        responseDistributionChartModel = ResponseDistributionChartModel(
                            interactionId = 11,
                            choiceSpecification = ChoiceSpecificationData(
                                2,
                                listOf(2)
                            ),
                            results = ResponsesDistribution(
                                ResponsesDistributionOnAttempt(4, arrayOf(1, 3), 0)
                            ).toLegacyFormat()
                        ),
                        confidenceDistributionChartModel = ConfidenceDistributionChartModel(
                            interactionId = 11,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(2)
                            ),
                            results = ConfidenceDistribution(
                                listOf(
                                    ConfidenceDistributionOnResponse(10, arrayOf(1, 3, 1, 5), 0),
                                    ConfidenceDistributionOnResponse(18, arrayOf(10, 3, 2, 2), 0),
                                    ConfidenceDistributionOnResponse(7, arrayOf(1, 1, 2, 4), 0),
                                    ConfidenceDistributionOnResponse(10, arrayOf(3, 1, 5, 1), 0)
                                )
                            ).toJSON()
                        ),
                        explanationViewerModel = ChoiceExplanationViewerModel(
                            explanationsByResponse = mapOf(
                                ResponseData(
                                    listOf(1),
                                    0,
                                    false
                                )
                                        to listOf(
                                    ExplanationData(
                                        responseId = Random.nextLong(),
                                        "explication 1",
                                        "Joe Walson (@Jwal)",
                                        confidenceDegree = ConfidenceDegree.CONFIDENT,
                                        score = BigDecimal("0"),
                                        choiceList = LearnerChoice(listOf(1))
                                    ),
                                    ExplanationData(
                                        responseId = Random.nextLong(),
                                        "explication 2",
                                        "Jack DiCaprio (@Jdic)",
                                        confidenceDegree = ConfidenceDegree.NOT_CONFIDENT_AT_ALL,
                                        score = BigDecimal("0"),
                                        choiceList = LearnerChoice(listOf(1))
                                    )
                                ),
                                ResponseData(
                                    listOf(2),
                                    100,
                                    true
                                )
                                        to listOf(
                                    ExplanationData(
                                        responseId = Random.nextLong(),
                                        "explication 3",
                                        "Wiliam Shakespeare (@Wsha)",
                                        confidenceDegree = ConfidenceDegree.NOT_REALLY_CONFIDENT,
                                        score = BigDecimal("100"),
                                        choiceList = LearnerChoice(listOf(2))
                                    ),
                                    ExplanationData(
                                        responseId = Random.nextLong(),
                                        "explication 4",
                                        "Averell Collignon (@Acol)",
                                        confidenceDegree = ConfidenceDegree.TOTALLY_CONFIDENT,
                                        score = BigDecimal("100"),
                                        choiceList = LearnerChoice(listOf(2))
                                    )
                                )
                            ),
                            recommendedExplanationsComparator = IncorrectAndConfidenceDegreeComparator()
                        )
                    )
                ),
                ResultsSituation(
                    description = "12. Sequence running, hasChoices, has results, has explanations, teacherExplanation",
                    resultsModel = ChoiceResultsModel(
                        sequenceIsStopped = false,
                        sequenceId = 12,
                        responseDistributionChartModel = ResponseDistributionChartModel(
                            interactionId = 12,
                            choiceSpecification = ChoiceSpecificationData(
                                2,
                                listOf(2)
                            ),
                            results = ResponsesDistribution(
                                ResponsesDistributionOnAttempt(4, arrayOf(1, 3), 0)
                            ).toLegacyFormat()
                        ),
                        confidenceDistributionChartModel = ConfidenceDistributionChartModel(
                            interactionId = 12,
                            choiceSpecification = ChoiceSpecificationData(
                                2,
                                listOf(2)
                            ),
                            results = ConfidenceDistribution(
                                listOf(
                                    ConfidenceDistributionOnResponse(1, arrayOf(0, 1, 0, 0), 0),
                                    ConfidenceDistributionOnResponse(3, arrayOf(1, 1, 1, 0), 0)
                                )
                            ).toJSON()
                        ),
                        explanationViewerModel = ChoiceExplanationViewerModel(
                            explanationsByResponse = mapOf(
                                ResponseData(
                                    listOf(1),
                                    0,
                                    false
                                )
                                        to listOf(
                                    ExplanationData(
                                        responseId = Random.nextLong(),
                                        "explication 1",
                                        "Joe Walson (@Jwal)",
                                        confidenceDegree = ConfidenceDegree.NOT_REALLY_CONFIDENT,
                                        score = BigDecimal("0")

                                    ),
                                    ExplanationData(
                                        responseId = Random.nextLong(),
                                        "explication 2",
                                        "Jack DiCaprio (@Jdic)",
                                        confidenceDegree = ConfidenceDegree.CONFIDENT,
                                        score = BigDecimal("0")
                                    ),
                                    ExplanationData(
                                        responseId = Random.nextLong(),
                                        "explication 3",
                                        "Jane Doe (@Jdoe)",
                                        confidenceDegree = ConfidenceDegree.TOTALLY_CONFIDENT,
                                        score = BigDecimal("0")
                                    )
                                ),
                                ResponseData(
                                    listOf(2),
                                    100,
                                    true
                                )
                                        to listOf(
                                    ExplanationData(
                                        responseId = Random.nextLong(),
                                        "explication 4",
                                        "Wiliam Shakespeare (@Wsha)",
                                        confidenceDegree = ConfidenceDegree.CONFIDENT,
                                        score = BigDecimal("100")
                                    ),
                                    ExplanationData(
                                        responseId = Random.nextLong(),
                                        "explication 5",
                                        "Averell Collignon (@Acol)",
                                        confidenceDegree = ConfidenceDegree.NOT_CONFIDENT_AT_ALL,
                                        score = BigDecimal("100")
                                    ),
                                    TeacherExplanationData(
                                        responseId = Random.nextLong(),
                                        "explication de l'enseignant",
                                        "Franck Sil (@fsil)",
                                        confidenceDegree = ConfidenceDegree.CONFIDENT,
                                        score = BigDecimal("100")
                                    )
                                )
                            )
                        )
                    )
                ),
                ResultsSituation(
                    description = "13. Sequence running, hasChoices, has results, has explanations, has evaluations, teacherExplanation",
                    resultsModel = ChoiceResultsModel(
                        sequenceIsStopped = false,
                        sequenceId = 13,
                        responseDistributionChartModel = ResponseDistributionChartModel(
                            interactionId = 13,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(2)
                            ),
                            results = ResponsesDistribution(
                                ResponsesDistributionOnAttempt(10, arrayOf(1, 3, 4, 2), 0)
                            ).toLegacyFormat()
                        ),
                        confidenceDistributionChartModel = ConfidenceDistributionChartModel(
                            interactionId = 13,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(2)
                            ),
                            results = ConfidenceDistribution(
                                listOf(
                                    ConfidenceDistributionOnResponse(1, arrayOf(0, 1, 0, 0), 0),
                                    ConfidenceDistributionOnResponse(3, arrayOf(0, 2, 1, 0), 0),
                                    ConfidenceDistributionOnResponse(4, arrayOf(3, 1, 0, 0), 0),
                                    ConfidenceDistributionOnResponse(2, arrayOf(1, 0, 1, 0), 0)
                                )
                            ).toJSON()
                        ),
                        evaluationDistributionChartModel = EvaluationDistributionChartModel(
                            interactionId = 13,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(2)
                            ),
                            results = GradingDistribution(
                                listOf(
                                    GradingDistributionOnResponse(15, arrayOf(1, 7, 1, 1, 5), 0),
                                    GradingDistributionOnResponse(12, arrayOf(1, 0, 2, 8, 1), 0),
                                    GradingDistributionOnResponse(10, arrayOf(3, 0, 1, 4, 2), 0),
                                    GradingDistributionOnResponse(3, arrayOf(1, 0, 1, 1, 0), 0)
                                )
                            ).toLegacyFormat()
                        ),
                        explanationViewerModel = ChoiceExplanationViewerModel(
                            explanationsByResponse = mapOf(
                                ResponseData(
                                    listOf(1),
                                    0,
                                    false
                                )
                                        to listOf(
                                    ExplanationData(
                                        responseId = Random.nextLong(),
                                        "explication 1",
                                        "Joe Walson (@Jwal)",
                                        4,
                                        4,
                                        BigDecimal("3.5"),
                                        confidenceDegree = ConfidenceDegree.CONFIDENT,
                                        score = BigDecimal("0"),
                                        choiceList = LearnerChoice(listOf(1))
                                    ),
                                    ExplanationData(
                                        responseId = Random.nextLong(),
                                        "explication 2",
                                        "Jack DiCaprio (@Jdic)",
                                        2,
                                        2,
                                        BigDecimal("1.5"),
                                        confidenceDegree = ConfidenceDegree.NOT_CONFIDENT_AT_ALL,
                                        score = BigDecimal("0"),
                                        choiceList = LearnerChoice(listOf(1))
                                    )
                                ),
                                ResponseData(
                                    listOf(2),
                                    100,
                                    true
                                )
                                        to listOf(
                                    TeacherExplanationData(
                                        responseId = Random.nextLong(),
                                        "explication de l'enseignant",
                                        "Franck Sil (@fsil)",
                                        3,
                                        3,
                                        BigDecimal("2.75"),
                                        confidenceDegree = ConfidenceDegree.CONFIDENT,
                                        score = BigDecimal("0"),
                                        choiceList = LearnerChoice(listOf(2)),
                                    ),
                                    ExplanationData(
                                        responseId = Random.nextLong(),
                                        "explication 3",
                                        "Wiliam Shakespeare (@Wsha)",
                                        1,
                                        1,
                                        BigDecimal("5"),
                                        confidenceDegree = ConfidenceDegree.NOT_REALLY_CONFIDENT,
                                        score = BigDecimal("100"),
                                        choiceList = LearnerChoice(listOf(2))
                                    ),
                                    ExplanationData(
                                        responseId = Random.nextLong(),
                                        "explication 4",
                                        "Averell Collignon (@Acol)",
                                        3,
                                        3,
                                        BigDecimal("1"),
                                        confidenceDegree = ConfidenceDegree.TOTALLY_CONFIDENT,
                                        score = BigDecimal("100"),
                                        choiceList = LearnerChoice(listOf(2))
                                    )
                                )
                            )
                        )
                    )
                ),
                ResultsSituation(
                    description = "14. Sequence running, hasChoices, has results, only teacherExplanation",
                    resultsModel = ChoiceResultsModel(
                        sequenceIsStopped = false,
                        sequenceId = 14,
                        responseDistributionChartModel = ResponseDistributionChartModel(
                            interactionId = 14,
                            choiceSpecification = ChoiceSpecificationData(
                                2,
                                listOf(2)
                            ),
                            results = ResponsesDistribution(
                                ResponsesDistributionOnAttempt(1, arrayOf(0, 1), 0)
                            ).toLegacyFormat()
                        ),
                        confidenceDistributionChartModel = ConfidenceDistributionChartModel(
                            interactionId = 14,
                            choiceSpecification = ChoiceSpecificationData(
                                2,
                                listOf(2)
                            ),
                            results = ConfidenceDistribution(
                                listOf(
                                    ConfidenceDistributionOnResponse(1, arrayOf(0, 0, 0, 0), 0),
                                    ConfidenceDistributionOnResponse(1, arrayOf(0, 0, 1, 0), 0)
                                )
                            ).toJSON()
                        ),
                        explanationViewerModel = ChoiceExplanationViewerModel(
                            explanationsByResponse = mapOf(
                                ResponseData(
                                    listOf(1),
                                    0,
                                    false
                                )
                                        to listOf(
                                ),
                                ResponseData(
                                    listOf(2),
                                    100,
                                    true
                                )
                                        to listOf(
                                    TeacherExplanationData(
                                        responseId = Random.nextLong(),
                                        "explication de l'enseignant",
                                        "Franck Sil (@fsil)",
                                        confidenceDegree = ConfidenceDegree.CONFIDENT,
                                        score = BigDecimal("100")
                                    )
                                )
                            )
                        )
                    )
                ),
            )
        )


        return "player/assignment/sequence/components/results/test-results"
    }

    data class ResultsSituation(
        val description: String,
        val resultsModel: ResultsModel
    )

    @GetMapping("/recommendation")
    fun testRecommendation(
        authentication: Authentication,
        model: Model
    ): String {
        val user: User = authentication.principal as User

        model.addAttribute("user", user)

        model.addAttribute(
            "recommendationSituations",
            listOf(
                RecommendationSituation(
                    description = "1. End sequence because there are no explanations",
                    resultsModel = ChoiceResultsModel(
                        sequenceIsStopped = false,
                        sequenceId = 1,
                        recommendationModel = RecommendationModel(
                            noCorrectExplanation = true,
                            message = messageBuilder.message("player.sequence.recommendation.skipPhase2.message"),
                            popupDetailedExplanation = PopupDetailedExplanation.NO_EXPLANATION_FOR_CORRECT_ANSWERS
                        )
                    ),
                    sequenceId = 1
                ),
                RecommendationSituation(
                    description = "2. End sequence because p1 > 70%",
                    resultsModel = ChoiceResultsModel(
                        sequenceIsStopped = false,
                        sequenceId = 2,
                        responseDistributionChartModel = ResponseDistributionChartModel(
                            interactionId = 2,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(2)
                            ),
                            results = ResponsesDistribution(
                                ResponsesDistributionOnAttempt(100, arrayOf(10, 83, 4, 3), 0)
                            ).toLegacyFormat()
                        ),
                        recommendationModel = RecommendationModel(
                            message = messageBuilder.message("player.sequence.recommendation.skipPhase2.weak_benefits.message"),
                            explanationP1 = ExplanationP1.VERY_HIGH,
                            popupDetailedExplanation = PopupDetailedExplanation.WEAK_BENEFITS
                        )
                    ),
                    sequenceId = 2
                ),
                RecommendationSituation(
                    description = "3. Skip phase 2 because p1 < 30% and pconf < 0",
                    resultsModel = ChoiceResultsModel(
                        sequenceIsStopped = false,
                        sequenceId = 3,
                        responseDistributionChartModel = ResponseDistributionChartModel(
                            interactionId = 3,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(2)
                            ),
                            results = ResponsesDistribution(
                                ResponsesDistributionOnAttempt(100, arrayOf(42, 12, 33, 13), 0)
                            ).toLegacyFormat()
                        ),
                        confidenceDistributionChartModel = ConfidenceDistributionChartModel(
                            interactionId = 3,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(2)
                            ),
                            results = ConfidenceDistribution(
                                listOf(
                                    ConfidenceDistributionOnResponse(42, arrayOf(10, 10, 12, 10), 0),
                                    ConfidenceDistributionOnResponse(12, arrayOf(8, 3, 0, 1), 0),
                                    ConfidenceDistributionOnResponse(33, arrayOf(0, 0, 0, 33), 0),
                                    ConfidenceDistributionOnResponse(13, arrayOf(3, 1, 5, 4), 0)
                                )
                            ).toJSON()
                        ),
                        recommendationModel = RecommendationModel(
                            message = messageBuilder.message("player.sequence.recommendation.skipPhase2.message"),
                            explanationP1 = ExplanationP1.TOO_LOW,
                            explanationPConf = ExplanationPConf.PCONF_NEG,
                            popupDetailedExplanation = PopupDetailedExplanation.NON_SIGNIFICANT_BENEFITS
                        )
                    ),
                    sequenceId = 3
                ),
                RecommendationSituation(
                    description = "4. Provide hint because p1 < 30% and pconf > 0",
                    resultsModel = ChoiceResultsModel(
                        sequenceIsStopped = false,
                        sequenceId = 4,
                        responseDistributionChartModel = ResponseDistributionChartModel(
                            interactionId = 4,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(2)
                            ),
                            results = ResponsesDistribution(
                                ResponsesDistributionOnAttempt(20, arrayOf(12, 3, 3, 2), 0)
                            ).toLegacyFormat()
                        ),
                        confidenceDistributionChartModel = ConfidenceDistributionChartModel(
                            interactionId = 4,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(2)
                            ),
                            results = ConfidenceDistribution(
                                listOf(
                                    ConfidenceDistributionOnResponse(12, arrayOf(3, 2, 3, 4), 0),
                                    ConfidenceDistributionOnResponse(3, arrayOf(0, 0, 0, 3), 0),
                                    ConfidenceDistributionOnResponse(3, arrayOf(3, 0, 0, 0), 0),
                                    ConfidenceDistributionOnResponse(2, arrayOf(1, 0, 0, 1), 0)
                                )
                            ).toJSON()
                        ),
                        recommendationModel = RecommendationModel(
                            message = messageBuilder.message("player.sequence.recommendation.provide_hint.message"),
                            explanationP1 = ExplanationP1.TOO_LOW,
                            explanationPConf = ExplanationPConf.PCONF_POS,
                            popupDetailedExplanation = PopupDetailedExplanation.WEAK_BENEFITS
                        )
                    ),
                    sequenceId = 4
                ),
                RecommendationSituation(
                    description = "5. Provide hint because p1 < 30% and pconf = 0",
                    resultsModel = ChoiceResultsModel(
                        sequenceIsStopped = false,
                        sequenceId = 5,
                        responseDistributionChartModel = ResponseDistributionChartModel(
                            interactionId = 5,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(2)
                            ),
                            results = ResponsesDistribution(
                                ResponsesDistributionOnAttempt(84, arrayOf(20, 14, 32, 18), 0)
                            ).toLegacyFormat()
                        ),
                        confidenceDistributionChartModel = ConfidenceDistributionChartModel(
                            interactionId = 5,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(2)
                            ),
                            results = ConfidenceDistribution(
                                listOf(
                                    ConfidenceDistributionOnResponse(20, arrayOf(8, 9, 0, 3), 0),
                                    ConfidenceDistributionOnResponse(14, arrayOf(4, 7, 1, 2), 0),
                                    ConfidenceDistributionOnResponse(32, arrayOf(10, 20, 0, 2), 0),
                                    ConfidenceDistributionOnResponse(18, arrayOf(2, 6, 5, 5), 0)
                                )
                            ).toJSON()
                        ),
                        recommendationModel = RecommendationModel(
                            message = messageBuilder.message("player.sequence.recommendation.provide_hint.message"),
                            explanationP1 = ExplanationP1.TOO_LOW,
                            explanationPConf = ExplanationPConf.PCONF_ZERO,
                            popupDetailedExplanation = PopupDetailedExplanation.WEAK_BENEFITS
                        )
                    ),
                    sequenceId = 5
                ),
                RecommendationSituation(
                    description = "6. Provide hint because p1 < 30% and pconf is null",
                    resultsModel = ChoiceResultsModel(
                        sequenceIsStopped = false,
                        sequenceId = 6,
                        responseDistributionChartModel = ResponseDistributionChartModel(
                            interactionId = 6,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(2)
                            ),
                            results = ResponsesDistribution(
                                ResponsesDistributionOnAttempt(10, arrayOf(4, 1, 3, 2), 0)
                            ).toLegacyFormat()
                        ),
                        recommendationModel = RecommendationModel(
                            message = messageBuilder.message("player.sequence.recommendation.provide_hint.message"),
                            explanationP1 = ExplanationP1.TOO_LOW,
                            popupDetailedExplanation = PopupDetailedExplanation.WEAK_BENEFITS
                        )
                    ),
                    sequenceId = 6
                ),
                RecommendationSituation(
                    description = "7. Phase 2 was skipped: no explanations for correct answers",
                    resultsModel = ChoiceResultsModel(
                        sequenceIsStopped = false,
                        sequenceId = 7,
                        responseDistributionChartModel = ResponseDistributionChartModel(
                            interactionId = 7,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(2)
                            ),
                            results = ResponsesDistribution(
                                ResponsesDistributionOnAttempt(10, arrayOf(4, 1, 3, 2), 0)
                            ).toLegacyFormat()
                        ),
                        recommendationModel = RecommendationModel(
                            noCorrectExplanation = true,
                            message = messageBuilder.message("player.sequence.recommendation.focus_on_incorrect_detailed"),
                            popupDetailedExplanation = PopupDetailedExplanation.POPULAR_ANSWERS_INCORRECT
                        )
                    ),
                    sequenceId = 7
                ),
                RecommendationSituation(
                    description = "8. Phase 2 was skipped: discussion must be brief and focus on incorrect answers because p1 > 70% and pConf < 0",
                    resultsModel = ChoiceResultsModel(
                        sequenceIsStopped = false,
                        sequenceId = 8,
                        responseDistributionChartModel = ResponseDistributionChartModel(
                            interactionId = 8,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(3)
                            ),
                            results = ResponsesDistribution(
                                ResponsesDistributionOnAttempt(100, arrayOf(10, 2, 81, 7), 0)
                            ).toLegacyFormat()
                        ),
                        confidenceDistributionChartModel = ConfidenceDistributionChartModel(
                            interactionId = 8,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(3)
                            ),
                            results = ConfidenceDistribution(
                                listOf(
                                    ConfidenceDistributionOnResponse(10, arrayOf(4, 3, 2, 1), 0),
                                    ConfidenceDistributionOnResponse(2, arrayOf(0, 2, 0, 0), 0),
                                    ConfidenceDistributionOnResponse(81, arrayOf(81, 0, 0, 0), 0),
                                    ConfidenceDistributionOnResponse(7, arrayOf(1, 2, 1, 3), 0)
                                )
                            ).toJSON()
                        ),
                        recommendationModel = RecommendationModel(
                            message = messageBuilder.message("player.sequence.recommendation.focus_on_incorrect_brief"),
                            explanationP1 = ExplanationP1.VERY_HIGH_SKIP,
                            explanationPConf = ExplanationPConf.PCONF_NEG_SKIP,
                            popupDetailedExplanation = PopupDetailedExplanation.POPULAR_ANSWERS_INCORRECT
                        )
                    ),
                    sequenceId = 8
                ),
                RecommendationSituation(
                    description = "9. Phase 2 was skipped: discussion must focus on incorrect answers because p1 > 70% and pConf > 0",
                    resultsModel = ChoiceResultsModel(
                        sequenceIsStopped = false,
                        sequenceId = 9,
                        responseDistributionChartModel = ResponseDistributionChartModel(
                            interactionId = 9,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(3)
                            ),
                            results = ResponsesDistribution(
                                ResponsesDistributionOnAttempt(100, arrayOf(10, 2, 81, 7), 0)
                            ).toLegacyFormat()
                        ),
                        confidenceDistributionChartModel = ConfidenceDistributionChartModel(
                            interactionId = 9,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(3)
                            ),
                            results = ConfidenceDistribution(
                                listOf(
                                    ConfidenceDistributionOnResponse(10, arrayOf(4, 3, 2, 1), 0),
                                    ConfidenceDistributionOnResponse(2, arrayOf(0, 2, 0, 0), 0),
                                    ConfidenceDistributionOnResponse(81, arrayOf(1, 2, 10, 68), 0),
                                    ConfidenceDistributionOnResponse(7, arrayOf(1, 2, 1, 3), 0)
                                )
                            ).toJSON()
                        ),
                        recommendationModel = RecommendationModel(
                            message = messageBuilder.message("player.sequence.recommendation.focus_on_correct_brief"),
                            explanationP1 = ExplanationP1.VERY_HIGH_SKIP,
                            explanationPConf = ExplanationPConf.PCONF_POS_SKIP,
                            popupDetailedExplanation = PopupDetailedExplanation.POPULAR_ANSWERS_CORRECT
                        )
                    ),
                    sequenceId = 9
                ),
                RecommendationSituation(
                    description = "10. Phase 2 was skipped: discussion must be detailed and focus on incorrect answers because p1 > 70% and pConf = 0",
                    resultsModel = ChoiceResultsModel(
                        sequenceIsStopped = false,
                        sequenceId = 10,
                        responseDistributionChartModel = ResponseDistributionChartModel(
                            interactionId = 10,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(2)
                            ),
                            results = ResponsesDistribution(
                                ResponsesDistributionOnAttempt(84, arrayOf(7, 70, 7, 0), 0)
                            ).toLegacyFormat()
                        ),
                        confidenceDistributionChartModel = ConfidenceDistributionChartModel(
                            interactionId = 10,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(2)
                            ),
                            results = ConfidenceDistribution(
                                listOf(
                                    ConfidenceDistributionOnResponse(7, arrayOf(0, 0, 3, 4), 0),
                                    ConfidenceDistributionOnResponse(70, arrayOf(10, 10, 20, 30), 0),
                                    ConfidenceDistributionOnResponse(7, arrayOf(2, 2, 1, 2), 0),
                                    ConfidenceDistributionOnResponse(0, arrayOf(0, 0, 0, 0), 0)
                                )
                            ).toJSON()
                        ),
                        recommendationModel = RecommendationModel(
                            message = messageBuilder.message("player.sequence.recommendation.focus_on_correct_brief"),
                            explanationP1 = ExplanationP1.VERY_HIGH_SKIP,
                            explanationPConf = ExplanationPConf.PCONF_ZERO_SKIP,
                            popupDetailedExplanation = PopupDetailedExplanation.POPULAR_ANSWERS_CORRECT
                        )
                    ),
                    sequenceId = 10
                ),
                RecommendationSituation(
                    description = "11. Phase 2 was skipped: discussion must focus on correct answers because p1 > 70% and pConf is null",
                    resultsModel = ChoiceResultsModel(
                        sequenceIsStopped = false,
                        sequenceId = 11,
                        responseDistributionChartModel = ResponseDistributionChartModel(
                            interactionId = 11,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(2)
                            ),
                            results = ResponsesDistribution(
                                ResponsesDistributionOnAttempt(84, arrayOf(7, 70, 7, 0), 0)
                            ).toLegacyFormat()
                        ),
                        recommendationModel = RecommendationModel(
                            message = messageBuilder.message("player.sequence.recommendation.focus_on_correct"),
                            explanationP1 = ExplanationP1.VERY_HIGH_SKIP,
                            popupDetailedExplanation = PopupDetailedExplanation.POPULAR_ANSWERS_CORRECT
                        )
                    ),
                    sequenceId = 11
                ),
                RecommendationSituation(
                    description = "12. Phase 2 was skipped: discussion must be detailed and focus on correct answers because p1 < 30% and pConf < 0",
                    resultsModel = ChoiceResultsModel(
                        sequenceIsStopped = false,
                        sequenceId = 12,
                        responseDistributionChartModel = ResponseDistributionChartModel(
                            interactionId = 12,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(2)
                            ),
                            results = ResponsesDistribution(
                                ResponsesDistributionOnAttempt(11, arrayOf(5, 1, 3, 2), 0)
                            ).toLegacyFormat()
                        ),
                        confidenceDistributionChartModel = ConfidenceDistributionChartModel(
                            interactionId = 12,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(2)
                            ),
                            results = ConfidenceDistribution(
                                listOf(
                                    ConfidenceDistributionOnResponse(5, arrayOf(1, 2, 1, 1), 0),
                                    ConfidenceDistributionOnResponse(1, arrayOf(1, 0, 0, 0), 0),
                                    ConfidenceDistributionOnResponse(3, arrayOf(1, 1, 0, 1), 0),
                                    ConfidenceDistributionOnResponse(2, arrayOf(0, 1, 0, 1), 0)
                                )
                            ).toJSON()
                        ),
                        recommendationModel = RecommendationModel(
                            message = messageBuilder.message("player.sequence.recommendation.focus_on_incorrect_detailed"),
                            explanationP1 = ExplanationP1.TOO_LOW_SKIP,
                            explanationPConf = ExplanationPConf.PCONF_NEG_SKIP,
                            popupDetailedExplanation = PopupDetailedExplanation.POPULAR_ANSWERS_INCORRECT
                        )
                    ),
                    sequenceId = 12
                ),
                RecommendationSituation(
                    description = "13. Phase 2 was skipped: discussion must be detailed and focus on incorrect answers because p1 < 30% and pConf > 0",
                    resultsModel = ChoiceResultsModel(
                        sequenceIsStopped = false,
                        sequenceId = 13,
                        responseDistributionChartModel = ResponseDistributionChartModel(
                            interactionId = 13,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(2)
                            ),
                            results = ResponsesDistribution(
                                ResponsesDistributionOnAttempt(11, arrayOf(5, 1, 3, 2), 0)
                            ).toLegacyFormat()
                        ),
                        confidenceDistributionChartModel = ConfidenceDistributionChartModel(
                            interactionId = 13,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(2)
                            ),
                            results = ConfidenceDistribution(
                                listOf(
                                    ConfidenceDistributionOnResponse(5, arrayOf(1, 2, 1, 1), 0),
                                    ConfidenceDistributionOnResponse(1, arrayOf(0, 0, 0, 1), 0),
                                    ConfidenceDistributionOnResponse(3, arrayOf(1, 1, 0, 1), 0),
                                    ConfidenceDistributionOnResponse(2, arrayOf(0, 1, 0, 1), 0)
                                )
                            ).toJSON()
                        ),
                        recommendationModel = RecommendationModel(
                            message = messageBuilder.message("player.sequence.recommendation.focus_on_correct_detailed"),
                            explanationP1 = ExplanationP1.TOO_LOW_SKIP,
                            explanationPConf = ExplanationPConf.PCONF_POS_SKIP,
                            popupDetailedExplanation = PopupDetailedExplanation.POPULAR_ANSWERS_CORRECT
                        )
                    ),
                    sequenceId = 13
                ),
                RecommendationSituation(
                    description = "14. Phase 2 was skipped: discussion must be detailed and focus on incorrect answers because p1 < 30% and pConf = 0",
                    resultsModel = ChoiceResultsModel(
                        sequenceIsStopped = false,
                        sequenceId = 14,
                        responseDistributionChartModel = ResponseDistributionChartModel(
                            interactionId = 14,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(2)
                            ),
                            results = ResponsesDistribution(
                                ResponsesDistributionOnAttempt(11, arrayOf(5, 1, 3, 2), 0)
                            ).toLegacyFormat()
                        ),
                        confidenceDistributionChartModel = ConfidenceDistributionChartModel(
                            interactionId = 14,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(2)
                            ),
                            results = ConfidenceDistribution(
                                listOf(
                                    ConfidenceDistributionOnResponse(5, arrayOf(0, 0, 0, 5), 0),
                                    ConfidenceDistributionOnResponse(1, arrayOf(0, 0, 0, 1), 0),
                                    ConfidenceDistributionOnResponse(3, arrayOf(0, 0, 0, 3), 0),
                                    ConfidenceDistributionOnResponse(2, arrayOf(0, 0, 0, 2), 0)
                                )
                            ).toJSON()
                        ),
                        recommendationModel = RecommendationModel(
                            message = messageBuilder.message("player.sequence.recommendation.focus_on_correct_detailed"),
                            explanationP1 = ExplanationP1.TOO_LOW_SKIP,
                            explanationPConf = ExplanationPConf.PCONF_ZERO_SKIP,
                            popupDetailedExplanation = PopupDetailedExplanation.POPULAR_ANSWERS_CORRECT
                        )
                    ),
                    sequenceId = 14
                ),
                RecommendationSituation(
                    description = "15. Phase 2 was skipped: discussion must be detailed and focus on incorrect answers because p1 < 30% and pConf is null",
                    resultsModel = ChoiceResultsModel(
                        sequenceIsStopped = false,
                        sequenceId = 15,
                        responseDistributionChartModel = ResponseDistributionChartModel(
                            interactionId = 15,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(2)
                            ),
                            results = ResponsesDistribution(
                                ResponsesDistributionOnAttempt(11, arrayOf(5, 1, 3, 2), 0)
                            ).toLegacyFormat()
                        ),
                        recommendationModel = RecommendationModel(
                            message = messageBuilder.message("player.sequence.recommendation.focus_on_incorrect"),
                            explanationP1 = ExplanationP1.TOO_LOW_SKIP,
                            popupDetailedExplanation = PopupDetailedExplanation.POPULAR_ANSWERS_INCORRECT
                        )
                    ),
                    sequenceId = 15
                ),
                RecommendationSituation(
                    description = "16. Phase 2 was played: discussion must be detailed and focus on correct answers because pPeer > 0 and d < 0",
                    resultsModel = ChoiceResultsModel(
                        sequenceIsStopped = false,
                        sequenceId = 16,
                        responseDistributionChartModel = ResponseDistributionChartModel(
                            interactionId = 16,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(2)
                            ),
                            results = ResponsesDistribution(
                                ResponsesDistributionOnAttempt(11, arrayOf(5, 1, 3, 2), 0),
                                ResponsesDistributionOnAttempt(11, arrayOf(5, 0, 4, 2), 0)
                            ).toLegacyFormat()
                        ),
                        evaluationDistributionChartModel = EvaluationDistributionChartModel(
                            interactionId = 16,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(2)
                            ),
                            results = GradingDistribution(
                                listOf(
                                    GradingDistributionOnResponse(10, arrayOf(2, 6, 2, 0, 0), 0),
                                    GradingDistributionOnResponse(2, arrayOf(0, 0, 0, 0, 2), 0),
                                    GradingDistributionOnResponse(6, arrayOf(2, 2, 0, 0, 2), 0),
                                    GradingDistributionOnResponse(4, arrayOf(0, 2, 0, 2, 0), 0)
                                )
                            ).toLegacyFormat()
                        ),
                        recommendationModel = RecommendationModel(
                            message = messageBuilder.message("player.sequence.recommendation.focus_on_correct_detailed"),
                            explanationD = ExplanationD.D_NEG,
                            explanationPPeer = ExplanationPPeer.PPEER_POS,
                            popupDetailedExplanation = PopupDetailedExplanation.POPULAR_ANSWERS_CORRECT
                        )
                    ),
                    sequenceId = 16
                ),
                RecommendationSituation(
                    description = "17. Phase 2 was played: discussion must focus on incorrect answers because pPeer < 0 and d = 0",
                    resultsModel = ChoiceResultsModel(
                        sequenceIsStopped = false,
                        sequenceId = 17,
                        responseDistributionChartModel = ResponseDistributionChartModel(
                            interactionId = 17,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(2)
                            ),
                            results = ResponsesDistribution(
                                ResponsesDistributionOnAttempt(45, arrayOf(16, 1, 14, 14), 0),
                                ResponsesDistributionOnAttempt(45, arrayOf(7, 1, 15, 22), 0)
                            ).toLegacyFormat()
                        ),
                        evaluationDistributionChartModel = EvaluationDistributionChartModel(
                            interactionId = 17,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(2)
                            ),
                            results = GradingDistribution(
                                listOf(
                                    GradingDistributionOnResponse(30, arrayOf(10, 5, 2, 13, 10), 0),
                                    GradingDistributionOnResponse(45, arrayOf(13, 8, 7, 13, 4), 0),
                                    GradingDistributionOnResponse(30, arrayOf(5, 1, 6, 7, 11), 0),
                                    GradingDistributionOnResponse(30, arrayOf(5, 10, 6, 6, 3), 0)
                                )
                            ).toLegacyFormat()
                        ),
                        recommendationModel = RecommendationModel(
                            message = messageBuilder.message("player.sequence.recommendation.focus_on_incorrect_detailed"),
                            explanationD = ExplanationD.D_ZERO,
                            explanationPPeer = ExplanationPPeer.PPEER_NEG,
                            popupDetailedExplanation = PopupDetailedExplanation.POPULAR_ANSWERS_INCORRECT
                        )
                    ),
                    sequenceId = 17
                ),
                RecommendationSituation(
                    description = "18. Phase 2 was played: discussion must be detailed and focus on correct answers because pPeer = 0 and d < 0",
                    resultsModel = ChoiceResultsModel(
                        sequenceIsStopped = false,
                        sequenceId = 18,
                        responseDistributionChartModel = ResponseDistributionChartModel(
                            interactionId = 18,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(2)
                            ),
                            results = ResponsesDistribution(
                                ResponsesDistributionOnAttempt(11, arrayOf(5, 1, 3, 2), 0),
                                ResponsesDistributionOnAttempt(11, arrayOf(5, 0, 4, 2), 0)
                            ).toLegacyFormat()
                        ),
                        evaluationDistributionChartModel = EvaluationDistributionChartModel(
                            interactionId = 18,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(2)
                            ),
                            results = GradingDistribution(
                                listOf(
                                    GradingDistributionOnResponse(15, arrayOf(0, 0, 0, 0, 15), 0),
                                    GradingDistributionOnResponse(3, arrayOf(0, 0, 0, 0, 3), 0),
                                    GradingDistributionOnResponse(9, arrayOf(0, 0, 0, 0, 9), 0),
                                    GradingDistributionOnResponse(6, arrayOf(0, 0, 0, 0, 6), 0)
                                )
                            ).toLegacyFormat()
                        ),
                        recommendationModel = RecommendationModel(
                            message = messageBuilder.message("player.sequence.recommendation.focus_on_correct_detailed"),
                            explanationD = ExplanationD.D_NEG,
                            explanationPPeer = ExplanationPPeer.PPEER_ZERO,
                            popupDetailedExplanation = PopupDetailedExplanation.POPULAR_ANSWERS_CORRECT
                        )
                    ),
                    sequenceId = 18
                ),
                RecommendationSituation(
                    description = "19. Phase 2 was played: discussion must focus on correct answers because pPeer > 0 and d > 0",
                    resultsModel = ChoiceResultsModel(
                        sequenceIsStopped = false,
                        sequenceId = 19,
                        responseDistributionChartModel = ResponseDistributionChartModel(
                            interactionId = 19,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(2)
                            ),
                            results = ResponsesDistribution(
                                ResponsesDistributionOnAttempt(45, arrayOf(16, 1, 14, 14), 0),
                                ResponsesDistributionOnAttempt(45, arrayOf(14, 3, 14, 14), 0)
                            ).toLegacyFormat()
                        ),
                        evaluationDistributionChartModel = EvaluationDistributionChartModel(
                            interactionId = 19,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(2)
                            ),
                            results = GradingDistribution(
                                listOf(
                                    GradingDistributionOnResponse(42, arrayOf(13, 23, 2, 5, 2), 0),
                                    GradingDistributionOnResponse(22, arrayOf(1, 4, 3, 1, 13), 0),
                                    GradingDistributionOnResponse(36, arrayOf(14, 10, 6, 0, 6), 0),
                                    GradingDistributionOnResponse(35, arrayOf(3, 19, 6, 1, 6), 0)
                                )
                            ).toLegacyFormat()
                        ),
                        recommendationModel = RecommendationModel(
                            message = messageBuilder.message("player.sequence.recommendation.focus_on_correct"),
                            explanationD = ExplanationD.D_POS,
                            explanationPPeer = ExplanationPPeer.PPEER_POS,
                            popupDetailedExplanation = PopupDetailedExplanation.POPULAR_ANSWERS_CORRECT
                        )
                    ),
                    sequenceId = 19
                ),
                RecommendationSituation(
                    description = "20. Phase 2 was played: discussion must focus on correct answers because pPeer = 0 and d > 0",
                    resultsModel = ChoiceResultsModel(
                        sequenceIsStopped = false,
                        sequenceId = 20,
                        responseDistributionChartModel = ResponseDistributionChartModel(
                            interactionId = 20,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(2)
                            ),
                            results = ResponsesDistribution(
                                ResponsesDistributionOnAttempt(45, arrayOf(16, 1, 14, 14), 0),
                                ResponsesDistributionOnAttempt(45, arrayOf(14, 3, 14, 14), 0)
                            ).toLegacyFormat()
                        ),
                        evaluationDistributionChartModel = EvaluationDistributionChartModel(
                            interactionId = 20,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(2)
                            ),
                            results = GradingDistribution(
                                listOf(
                                    GradingDistributionOnResponse(30, arrayOf(10, 5, 2, 13, 10), 0),
                                    GradingDistributionOnResponse(45, arrayOf(10, 8, 7, 13, 7), 0),
                                    GradingDistributionOnResponse(30, arrayOf(5, 1, 6, 7, 11), 0),
                                    GradingDistributionOnResponse(30, arrayOf(5, 10, 6, 6, 3), 0)
                                )
                            ).toLegacyFormat()
                        ),
                        recommendationModel = RecommendationModel(
                            message = messageBuilder.message("player.sequence.recommendation.focus_on_correct"),
                            explanationD = ExplanationD.D_POS,
                            explanationPPeer = ExplanationPPeer.PPEER_ZERO,
                            popupDetailedExplanation = PopupDetailedExplanation.POPULAR_ANSWERS_CORRECT
                        )
                    ),
                    sequenceId = 20
                ),
                RecommendationSituation(
                    description = "21. Phase 2 was played: discussion must be focus on correct answers because pPeer > 0 and d = 0",
                    resultsModel = ChoiceResultsModel(
                        sequenceIsStopped = false,
                        sequenceId = 21,
                        responseDistributionChartModel = ResponseDistributionChartModel(
                            interactionId = 21,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(2)
                            ),
                            results = ResponsesDistribution(
                                ResponsesDistributionOnAttempt(45, arrayOf(16, 1, 14, 14), 0),
                                ResponsesDistributionOnAttempt(45, arrayOf(16, 1, 14, 14), 0)
                            ).toLegacyFormat()
                        ),
                        evaluationDistributionChartModel = EvaluationDistributionChartModel(
                            interactionId = 21,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(2)
                            ),
                            results = GradingDistribution(
                                listOf(
                                    GradingDistributionOnResponse(42, arrayOf(13, 23, 2, 5, 2), 0),
                                    GradingDistributionOnResponse(22, arrayOf(1, 4, 3, 1, 13), 0),
                                    GradingDistributionOnResponse(36, arrayOf(14, 10, 6, 0, 6), 0),
                                    GradingDistributionOnResponse(35, arrayOf(3, 19, 6, 1, 6), 0)
                                )
                            ).toLegacyFormat()
                        ),
                        recommendationModel = RecommendationModel(
                            message = messageBuilder.message("player.sequence.recommendation.focus_on_correct_detailed"),
                            explanationD = ExplanationD.D_ZERO,
                            explanationPPeer = ExplanationPPeer.PPEER_POS,
                            popupDetailedExplanation = PopupDetailedExplanation.POPULAR_ANSWERS_CORRECT
                        )
                    ),
                    sequenceId = 21
                ),
                RecommendationSituation(
                    description = "22. Phase 2 was played: discussion focus on correct answers because pPeer = 0 and d = 0",
                    resultsModel = ChoiceResultsModel(
                        sequenceIsStopped = false,
                        sequenceId = 22,
                        responseDistributionChartModel = ResponseDistributionChartModel(
                            interactionId = 22,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(2)
                            ),
                            results = ResponsesDistribution(
                                ResponsesDistributionOnAttempt(45, arrayOf(16, 1, 14, 14), 0),
                                ResponsesDistributionOnAttempt(45, arrayOf(16, 1, 14, 14), 0)
                            ).toLegacyFormat()
                        ),
                        evaluationDistributionChartModel = EvaluationDistributionChartModel(
                            interactionId = 22,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(2)
                            ),
                            results = GradingDistribution(
                                listOf(
                                    GradingDistributionOnResponse(30, arrayOf(10, 5, 2, 13, 10), 0),
                                    GradingDistributionOnResponse(45, arrayOf(10, 8, 7, 13, 7), 0),
                                    GradingDistributionOnResponse(30, arrayOf(5, 1, 6, 7, 11), 0),
                                    GradingDistributionOnResponse(30, arrayOf(5, 10, 6, 6, 3), 0)
                                )
                            ).toLegacyFormat()
                        ),
                        recommendationModel = RecommendationModel(
                            message = messageBuilder.message("player.sequence.recommendation.focus_on_correct_detailed"),
                            explanationD = ExplanationD.D_ZERO,
                            explanationPPeer = ExplanationPPeer.PPEER_ZERO,
                            popupDetailedExplanation = PopupDetailedExplanation.POPULAR_ANSWERS_CORRECT
                        )
                    ),
                    sequenceId = 22
                ),
                RecommendationSituation(
                    description = "23. Phase 2 was played: discussion must be detailed and focus on incorrect answers because pPeer < 0 and d < 0",
                    resultsModel = ChoiceResultsModel(
                        sequenceIsStopped = false,
                        sequenceId = 23,
                        responseDistributionChartModel = ResponseDistributionChartModel(
                            interactionId = 23,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(2)
                            ),
                            results = ResponsesDistribution(
                                ResponsesDistributionOnAttempt(45, arrayOf(16, 1, 14, 14), 0),
                                ResponsesDistributionOnAttempt(45, arrayOf(16, 0, 15, 14), 0)
                            ).toLegacyFormat()
                        ),
                        evaluationDistributionChartModel = EvaluationDistributionChartModel(
                            interactionId = 23,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(2)
                            ),
                            results = GradingDistribution(
                                listOf(
                                    GradingDistributionOnResponse(30, arrayOf(10, 5, 2, 13, 10), 0),
                                    GradingDistributionOnResponse(45, arrayOf(13, 8, 7, 13, 4), 0),
                                    GradingDistributionOnResponse(30, arrayOf(5, 1, 6, 7, 11), 0),
                                    GradingDistributionOnResponse(30, arrayOf(5, 10, 6, 6, 3), 0)
                                )
                            ).toLegacyFormat()
                        ),
                        recommendationModel = RecommendationModel(
                            message = messageBuilder.message("player.sequence.recommendation.focus_on_incorrect_detailed"),
                            explanationD = ExplanationD.D_NEG,
                            explanationPPeer = ExplanationPPeer.PPEER_NEG,
                            popupDetailedExplanation = PopupDetailedExplanation.POPULAR_ANSWERS_INCORRECT
                        )
                    ),
                    sequenceId = 23
                ),
                RecommendationSituation(
                    description = "24. Phase 2 was played: discussion must focus on incorrect answers because pPeer < 0 and d > 0",
                    resultsModel = ChoiceResultsModel(
                        sequenceIsStopped = false,
                        sequenceId = 24,
                        responseDistributionChartModel = ResponseDistributionChartModel(
                            interactionId = 24,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(2)
                            ),
                            results = ResponsesDistribution(
                                ResponsesDistributionOnAttempt(45, arrayOf(16, 1, 14, 14), 0),
                                ResponsesDistributionOnAttempt(45, arrayOf(7, 10, 15, 14), 0)
                            ).toLegacyFormat()
                        ),
                        evaluationDistributionChartModel = EvaluationDistributionChartModel(
                            interactionId = 24,
                            choiceSpecification = ChoiceSpecificationData(
                                4,
                                listOf(2)
                            ),
                            results = GradingDistribution(
                                listOf(
                                    GradingDistributionOnResponse(30, arrayOf(10, 5, 2, 13, 10), 0),
                                    GradingDistributionOnResponse(45, arrayOf(13, 8, 7, 13, 4), 0),
                                    GradingDistributionOnResponse(30, arrayOf(5, 1, 6, 7, 11), 0),
                                    GradingDistributionOnResponse(30, arrayOf(5, 10, 6, 6, 3), 0)
                                )
                            ).toLegacyFormat()
                        ),
                        recommendationModel = RecommendationModel(
                            message = messageBuilder.message("player.sequence.recommendation.focus_on_incorrect"),
                            explanationD = ExplanationD.D_POS,
                            explanationPPeer = ExplanationPPeer.PPEER_NEG,
                            popupDetailedExplanation = PopupDetailedExplanation.POPULAR_ANSWERS_INCORRECT
                        )
                    ),
                    sequenceId = 24
                )
            )
        )

        return "/player/assignment/sequence/components/test-recommendation"
    }

    data class RecommendationSituation(
        val description: String,
        val resultsModel: ResultsModel?,
        val sequenceId: Long
    )

    @GetMapping("/sequence-info")
    fun testSequenceInfo(
        authentication: Authentication,
        model: Model
    ): String {

        val user: User = authentication.principal as User

        model.addAttribute("user", user)
        model.addAttribute(
            "sequenceInfoSituations",
            SequenceGenerator.generateAllTypes(user)
                .map {
                    SequenceInfoSituation(
                        describeSequence(it),
                        SequenceInfoResolver.resolve(true, it, messageBuilder)
                    )
                }
        )

        return "player/assignment/sequence/components/sequence-info/test-sequence-info"
    }

    data class SequenceInfoSituation(
        val description: String,
        val model: SequenceInfoModel
    )

    private fun describeSequence(sequence: Sequence): String =
        "sequenceState=${sequence.state}, " +
                "executionContext=${sequence.executionContext}, " +
                "resultsArePublished=${sequence.resultsArePublished}" +

                (sequence.activeInteraction?.let {
                    ", interaction=(${it.interactionType},${it.state} )"
                } ?: "")


    @GetMapping("/command")
    fun testCommand(
        authentication: Authentication,
        model: Model
    ): String {
        val user: User = authentication.principal as User

        model.addAttribute("user", user)
        model.addAttribute(
            "commandSituations",
            SequenceGenerator.generateAllTypes(user)
                .map {
                    CommandSituation(
                        describeSequence(it),
                        CommandModelFactory.build(user, it)
                    )
                }
        )

        return "player/assignment/sequence/components/command/test-command"
    }

    data class CommandSituation(
        val description: String,
        val model: CommandModel
    )

    @GetMapping("/response-phase")
    fun testResponsePhase(
        authentication: Authentication,
        model: Model
    ): String {
        val user: User = authentication.principal as User

        model.addAttribute("user", user)
        model.addAttribute(
            "responsePhaseModel",
            LearnerResponsePhaseViewModel(
                sequenceId = 622L,
                interactionId = 1731L,
                learnerPhaseState = State.show,
                responseSubmitted = false,
                responseFormModel = LearnerResponseFormViewModel(
                    interactionId = 1731L,
                    attempt = 1,
                    responseSubmissionSpecification = ResponseSubmissionSpecification(
                        studentsProvideExplanation = true,
                        studentsProvideConfidenceDegree = true
                    ),
                    hasChoices = true,
                    multipleChoice = true,
                    firstAttemptChoices = arrayOf(2),
                    firstAttemptExplanation = "Hello World",
                    firstAttemptConfidenceDegree = ConfidenceDegree.CONFIDENT,
                    nbItem = 3
                )

            )
        )

        return "player/assignment/sequence/phase/response/test-response-phase"
    }

    @GetMapping("/evaluation-phase/all-at-once")
    fun testEvaluationPhaseAllAtOnce(
        authentication: Authentication,
        model: Model
    ): String {
        val user: User = authentication.principal as User

        model.addAttribute("user", user)
        model.addAttribute("phaseTemplate", AllAtOnceLearnerEvaluationPhase.TEMPLATE)

        model.addAttribute(
            "evaluationPhaseViewModel",
            AllAtOnceLearnerEvaluationPhaseViewModel(
                sequenceId = 12,
                interactionId = 123,
                choices = true,
                userHasCompletedPhase2 = false,
                responsesToGrade = listOf(
                    ExclusiveChoiceResponseData(
                        id = 1,
                        choice = 1,
                        explanation = "1st explanation"
                    ),
                    MultipleChoiceResponseData(
                        id = 2,
                        choices = listOf(2, 3),
                        explanation = "2nd explanation"
                    ),
                    OpenEndedResponseData(
                        id = 4,
                        explanation = "3rd explanation"
                    ),
                ),
                secondAttemptAllowed = true,
                secondAttemptAlreadySubmitted = false,
                responseFormModel = LearnerResponseFormViewModel(
                    interactionId = 1731L,
                    attempt = 2,
                    responseSubmissionSpecification = ResponseSubmissionSpecification(
                        studentsProvideExplanation = true,
                        studentsProvideConfidenceDegree = true
                    ),
                    hasChoices = true,
                    multipleChoice = true,
                    firstAttemptChoices = arrayOf(2),
                    firstAttemptExplanation = "Hello World",
                    firstAttemptConfidenceDegree = ConfidenceDegree.CONFIDENT,
                    nbItem = 3
                ),
                phaseState = State.show,
                userHasPerformedEvaluation = false
            )
        )

        return "player/assignment/sequence/phase/evaluation/test-evaluation-phase"
    }

    @GetMapping("/evaluation-phase/draxo")
    fun testEvaluationPhaseDraxo(
        authentication: Authentication,
        model: Model
    ): String {
        val user: User = authentication.principal as User

        model.addAttribute("user", user)
        model.addAttribute("phaseTemplate", DraxoLearnerEvaluationPhase.TEMPLATE)

        model.addAttribute(
            "evaluationPhaseViewModel",
            DraxoLearnerEvaluationPhaseViewModel(
                sequenceId = 12,
                interactionId = 123,
                choices = true,
                userHasCompletedPhase2 = false,
                nextResponseToGrade =
                ExclusiveChoiceResponseData(
                    id = 1,
                    choice = 1,
                    explanation = "1st explanation"
                ),
                lastResponseToGrade = false,
                secondAttemptAllowed = true,
                secondAttemptAlreadySubmitted = false,
                responseFormModel = LearnerResponseFormViewModel(
                    interactionId = 1731L,
                    attempt = 2,
                    responseSubmissionSpecification = ResponseSubmissionSpecification(
                        studentsProvideExplanation = true,
                        studentsProvideConfidenceDegree = true
                    ),
                    hasChoices = true,
                    multipleChoice = true,
                    firstAttemptChoices = arrayOf(2),
                    firstAttemptExplanation = "Hello World",
                    firstAttemptConfidenceDegree = ConfidenceDegree.CONFIDENT,
                    nbItem = 3
                ),
                phaseState = State.show,
            )
        )

        return "player/assignment/sequence/phase/evaluation/test-evaluation-phase"
    }

    @GetMapping("/recommendations")
    fun testRecommendations(): ResponseEntity<String> {
        val recommendationIsActive = featureManager.isActive(Feature { ElaasticFeatures.RECOMMENDATIONS.name })

        return ResponseEntity.ok("Recommendation feature : ${if (recommendationIsActive) "active" else "Inactive"}")

    }

    @GetMapping("/evaluation-phase/draxo-form")
    fun testDraxoForm(
        authentication: Authentication,
        model: Model
    ): String {

        val user: User = authentication.principal as User
        val draxoEvaluation = DraxoEvaluation()
            .addEvaluation(Criteria.D, OptionId.YES)
            .addEvaluation(Criteria.R, OptionId.NO, "Off-topic !")

        model.addAttribute("user", user)
        model.addAttribute("sequenceId", 123L)
        model.addAttribute("responseId", 456L)
        model.addAttribute("draxoEvaluation", draxoEvaluation)

        return "player/assignment/sequence/phase/evaluation/method/draxo/test-draxo-form"
    }

    @GetMapping("/evaluation-phase/draxo-show")
    fun testDraxoShow(
        authentication: Authentication,
        model: Model
    ): String {
        val user: User = authentication.principal as User
        val draxoEvaluation = DraxoEvaluation()
            .addEvaluation(Criteria.D, OptionId.YES)
            .addEvaluation(Criteria.R, OptionId.NO, "Off-topic !")

        model.addAttribute("user", user)
        model.addAttribute(
            "model",
            DraxoEvaluationModel(
                graderName = "Joe",
                graderNum = 1,
                score = DraxoGrading.computeGrade(draxoEvaluation),
                draxoEvaluation,

                )
        )

        return "player/assignment/sequence/phase/evaluation/method/draxo/test-draxo-show"
    }

    @GetMapping("/evaluation-phase/draxo-show-list")
    fun testDraxoShowList(
        authentication: Authentication,
        model: Model
    ): String {
        val user: User = authentication.principal as User
        val draxoEvaluationList =
            listOf(
                DraxoEvaluation()
                    .addEvaluation(Criteria.D, OptionId.NO, "I don't get it..."),

                DraxoEvaluation()
                    .addEvaluation(Criteria.D, OptionId.PARTIALLY, "I partially get it..."),

                DraxoEvaluation()
                    .addEvaluation(Criteria.D, OptionId.YES)
                    .addEvaluation(Criteria.R, OptionId.NO, "Off-topic !"),

                DraxoEvaluation()
                    .addEvaluation(Criteria.D, OptionId.YES)
                    .addEvaluation(Criteria.R, OptionId.PARTIALLY, "Partially off-topic..."),

                DraxoEvaluation()
                    .addEvaluation(Criteria.D, OptionId.YES)
                    .addEvaluation(Criteria.R, OptionId.DONT_KNOW),

                DraxoEvaluation()
                    .addEvaluation(Criteria.D, OptionId.YES)
                    .addEvaluation(Criteria.R, OptionId.YES)
                    .addEvaluation(Criteria.A, OptionId.NO, "I disagree"),

                DraxoEvaluation()
                    .addEvaluation(Criteria.D, OptionId.YES)
                    .addEvaluation(Criteria.R, OptionId.YES)
                    .addEvaluation(Criteria.A, OptionId.PARTIALLY, "I partially agree"),

                DraxoEvaluation()
                    .addEvaluation(Criteria.D, OptionId.YES)
                    .addEvaluation(Criteria.R, OptionId.YES)
                    .addEvaluation(Criteria.A, OptionId.NO_OPINION),

                DraxoEvaluation()
                    .addEvaluation(Criteria.D, OptionId.YES)
                    .addEvaluation(Criteria.R, OptionId.YES)
                    .addEvaluation(Criteria.A, OptionId.YES)
                    .addEvaluation(Criteria.X, OptionId.NO, "Man, it's incomplete"),

                DraxoEvaluation()
                    .addEvaluation(Criteria.D, OptionId.YES)
                    .addEvaluation(Criteria.R, OptionId.YES)
                    .addEvaluation(Criteria.A, OptionId.YES)
                    .addEvaluation(Criteria.X, OptionId.DONT_KNOW),

                DraxoEvaluation()
                    .addEvaluation(Criteria.D, OptionId.YES)
                    .addEvaluation(Criteria.R, OptionId.YES)
                    .addEvaluation(Criteria.A, OptionId.YES)
                    .addEvaluation(Criteria.X, OptionId.YES)
                    .addEvaluation(Criteria.O, OptionId.YES, "I know how to do better"),

                DraxoEvaluation()
                    .addEvaluation(Criteria.D, OptionId.YES)
                    .addEvaluation(Criteria.R, OptionId.YES)
                    .addEvaluation(Criteria.A, OptionId.YES)
                    .addEvaluation(Criteria.X, OptionId.YES)
                    .addEvaluation(Criteria.O, OptionId.YES, "I just have no idea"),

                DraxoEvaluation()
                    .addEvaluation(Criteria.D, OptionId.YES)
                    .addEvaluation(Criteria.R, OptionId.YES)
                    .addEvaluation(Criteria.A, OptionId.YES)
                    .addEvaluation(Criteria.X, OptionId.YES)
                    .addEvaluation(Criteria.O, OptionId.NO),
            )



        model.addAttribute("user", user)
        model.addAttribute(
            "evaluationModelList",
            draxoEvaluationList.mapIndexed() { i, draxoEvaluation ->
                EvaluationModel(
                    listOf(
                        DraxoEvaluationModel(
                            (i + 1).toString(),
                            (i + 1),
                            DraxoGrading.computeGrade(draxoEvaluation),
                            draxoEvaluation,
                            canReactOnPeerGrading = true,
                            canHidePeerGrading = true
                        )
                    ),
                    null,
                    false,
                    true
                )
            }
        )
        return "player/assignment/sequence/phase/evaluation/method/draxo/test-draxo-show-list"
    }

    @GetMapping("/chat-gpt-evaluation")
    fun testChatGptResponse(
        authentication: Authentication,
        model: Model
    ): String {

        val user: User = authentication.principal as User

        model.addAttribute("user", user)
        model.addAttribute(
            "chatGptEvaluationSituations",
            listOf(
                ChatGptEvaluationSituations(
                    description = "Annotation with grade",
                    model = ChatGptEvaluationModel(
                        0,
                        "La réponse semble manquer de précision et de soutien factuel. Il est essentiel de fournir des arguments solides et étayés par des preuves tangibles afin de renforcer ta position. De plus, certaines de tes affirmations sont en contradiction avec les connaissances actuelles sur le sujet, ce qui peut entraîner une confusion pour les autres participants. Je t'encourage à approfondir tes recherches et à consulter des sources fiables pour obtenir des informations précises et actualisées.",
                        BigDecimal(3),
                        ChatGptEvaluationStatus.DONE.name,
                        0,
                        canHideGrading = true
                    )
                ),
                ChatGptEvaluationSituations(
                    description = "Annotation with grade Hidden",
                    model = ChatGptEvaluationModel(
                        10,
                        "La réponse semble manquer de précision et de soutien factuel. Il est essentiel de fournir des arguments solides et étayés par des preuves tangibles afin de renforcer ta position. De plus, certaines de tes affirmations sont en contradiction avec les connaissances actuelles sur le sujet, ce qui peut entraîner une confusion pour les autres participants. Je t'encourage à approfondir tes recherches et à consulter des sources fiables pour obtenir des informations précises et actualisées.",
                        BigDecimal(3),
                        ChatGptEvaluationStatus.DONE.name,
                        0,
                        hiddenByTeacher = true,
                        canHideGrading = true
                    )
                ),
                ChatGptEvaluationSituations(
                    description = "Only annotation",
                    model = ChatGptEvaluationModel(
                        1,
                        "La réponse semble manquer de précision et de soutien factuel. Il est essentiel de fournir des arguments solides et étayés par des preuves tangibles afin de renforcer ta position. De plus, certaines de tes affirmations sont en contradiction avec les connaissances actuelles sur le sujet, ce qui peut entraîner une confusion pour les autres participants. Je t'encourage à approfondir tes recherches et à consulter des sources fiables pour obtenir des informations précises et actualisées.",
                        null,
                        ChatGptEvaluationStatus.DONE.name,
                        0,
                        canHideGrading = true
                    )
                ),
                ChatGptEvaluationSituations(
                    description = "Not Found",
                    model = ChatGptEvaluationModel(
                        2,
                        null,
                        null,
                        null,
                        0,
                        canHideGrading = true
                    )
                ),
                ChatGptEvaluationSituations(
                    description = "In progress",
                    model = ChatGptEvaluationModel(
                        3,
                        null,
                        null,
                        ChatGptEvaluationStatus.PENDING.name,
                        0,
                        canHideGrading = true
                    )
                ),
                ChatGptEvaluationSituations(
                    description = "Error",
                    model = ChatGptEvaluationModel(
                        4,
                        null,
                        null,
                        ChatGptEvaluationStatus.ERROR.name,
                        0,
                        canHideGrading = true
                    )
                )
            )
        )

        return "player/assignment/sequence/components/chat-gpt-evaluation/test-chat-gpt-evaluation-viewer"
    }

    data class ChatGptEvaluationSituations(
        val description: String,
        val model: ChatGptEvaluationModel
    )

    @GetMapping("/explanation-details")
    fun testExplanationDetails(
        authentication: Authentication,
        model: Model
    ): String {

        val user: User = authentication.principal as User

        model.addAttribute("user", user)
        model.addAttribute(
            "explanationDetailsSituations",
            listOf(
                explanationDetailsSituations(
                    description = "",
                    chatGptEvaluationModel = ChatGptEvaluationModel(
                        0,
                        "La réponse semble manquer de précision et de soutien factuel. Il est essentiel de fournir des arguments solides et étayés par des preuves tangibles afin de renforcer ta position. De plus, certaines de tes affirmations sont en contradiction avec les connaissances actuelles sur le sujet, ce qui peut entraîner une confusion pour les autres participants. Je t'encourage à approfondir tes recherches et à consulter des sources fiables pour obtenir des informations précises et actualisées.",
                        BigDecimal(3),
                        ChatGptEvaluationStatus.DONE.name,
                        0,
                    ),
                    learnerResultsModel = LearnerExclusiveChoiceResults(
                        explanationFirstTry = ExplanationData(responseId = Random.nextLong(), content = "I was wrong"),
                        explanationSecondTry = ExplanationData(
                            responseId = Random.nextLong(),
                            content = "And I've changed my mind"
                        ),
                        choiceFirstTry = LearnerChoice(listOf(2)),
                        choiceSecondTry = LearnerChoice(listOf(1)),
                        scoreFirstTry = 0,
                        scoreSecondTry = 100,
                        expectedChoice = ExclusiveChoiceSpecification(
                            nbCandidateItem = 4,
                            expectedChoice = ChoiceItem(1, 1.0f)
                        )
                    )
                )
            )
        )

        return "player/assignment/sequence/components/explanation-details/test-explanation-details"
    }

    data class explanationDetailsSituations(
        val description: String,
        val chatGptEvaluationModel: ChatGptEvaluationModel,
        val learnerResultsModel: LearnerResultsModel
    )


    @GetMapping("/dashboard/attendees-table")
    fun testDashboardAttendeesTable(
        authentication: Authentication,
        model: Model
    ): String {
        val user: User = authentication.principal as User

        /////Function and class use for this test case////

        /** Represent a situation for a table */
        data class TableAttendeesSituation(
            val description: String,
            val learnersMonitoringModel: SequenceMonitoringModel,
        )

        fun learnersMonitoringModel1(): SequenceMonitoringModel {
            val learnersMonitoringModel: SequenceMonitoringModel = SequenceMonitoringModel(
                ExecutionContext.FaceToFace,
                DashboardPhaseState.IN_PROGRESS,
                DashboardPhaseState.NOT_STARTED,
                mutableListOf()
            )
            val learners = mutableListOf(
                LearnerMonitoringModel(
                    1,
                    "B - I've submitted my answer",
                    LearnerStateOnPhase.ACTIVITY_TERMINATED,
                    LearnerStateOnPhase.ACTIVITY_NOT_TERMINATED,
                    LearnerStateOnPhase.ACTIVITY_NOT_TERMINATED,
                    learnersMonitoringModel
                ),
                LearnerMonitoringModel(
                    1,
                    "A - John Doe",
                    LearnerStateOnPhase.ACTIVITY_TERMINATED,
                    LearnerStateOnPhase.ACTIVITY_NOT_TERMINATED,
                    LearnerStateOnPhase.ACTIVITY_NOT_TERMINATED,
                    learnersMonitoringModel
                ),
                LearnerMonitoringModel(
                    3,
                    "I didn't answer yet",
                    LearnerStateOnPhase.ACTIVITY_NOT_TERMINATED,
                    LearnerStateOnPhase.ACTIVITY_NOT_TERMINATED,
                    LearnerStateOnPhase.ACTIVITY_NOT_TERMINATED,
                    learnersMonitoringModel
                ),
            )
            learnersMonitoringModel.setLearners(learners)
            return learnersMonitoringModel
        }

        fun learnersMonitoringModel2(): SequenceMonitoringModel {
            val learnersMonitoringModel: SequenceMonitoringModel = SequenceMonitoringModel(
                ExecutionContext.FaceToFace,
                DashboardPhaseState.NOT_STARTED,
                DashboardPhaseState.NOT_STARTED,
                mutableListOf()
            )
            val learners = mutableListOf(
                LearnerMonitoringModel(
                    1,
                    "The sequence is not started",
                    LearnerStateOnPhase.ACTIVITY_NOT_TERMINATED,
                    LearnerStateOnPhase.ACTIVITY_NOT_TERMINATED,
                    LearnerStateOnPhase.ACTIVITY_NOT_TERMINATED,
                    learnersMonitoringModel
                ),
            )
            learnersMonitoringModel.setLearners(learners)
            return learnersMonitoringModel
        }

        fun learnersMonitoringModel3(): SequenceMonitoringModel {
            val learnersMonitoringModel: SequenceMonitoringModel = SequenceMonitoringModel(
                ExecutionContext.Distance,
                DashboardPhaseState.IN_PROGRESS,
                DashboardPhaseState.IN_PROGRESS,
                mutableListOf()
            )
            val learners = mutableListOf(
                LearnerMonitoringModel(
                    4,
                    "B - I've finished this sequence",
                    LearnerStateOnPhase.ACTIVITY_TERMINATED,
                    LearnerStateOnPhase.ACTIVITY_TERMINATED,
                    LearnerStateOnPhase.ACTIVITY_TERMINATED,
                    learnersMonitoringModel
                ),
                LearnerMonitoringModel(
                    5,
                    "A - I've finished this sequence",
                    LearnerStateOnPhase.ACTIVITY_TERMINATED,
                    LearnerStateOnPhase.ACTIVITY_TERMINATED,
                    LearnerStateOnPhase.ACTIVITY_TERMINATED,
                    learnersMonitoringModel
                ),
                LearnerMonitoringModel(
                    3,
                    "C - I've submitted my response and my evaluation",
                    LearnerStateOnPhase.ACTIVITY_TERMINATED,
                    LearnerStateOnPhase.ACTIVITY_TERMINATED,
                    LearnerStateOnPhase.ACTIVITY_NOT_TERMINATED,
                    learnersMonitoringModel
                ),
                LearnerMonitoringModel(
                    2,
                    "D - I've submitted my response",
                    LearnerStateOnPhase.ACTIVITY_TERMINATED,
                    LearnerStateOnPhase.ACTIVITY_NOT_TERMINATED,
                    LearnerStateOnPhase.ACTIVITY_NOT_TERMINATED,
                    learnersMonitoringModel
                ),
                LearnerMonitoringModel(
                    1,
                    "B - I didn't start",
                    LearnerStateOnPhase.ACTIVITY_NOT_TERMINATED,
                    LearnerStateOnPhase.ACTIVITY_NOT_TERMINATED,
                    LearnerStateOnPhase.ACTIVITY_NOT_TERMINATED,
                    learnersMonitoringModel
                ),
            )

            learnersMonitoringModel.setLearners(learners)

            return learnersMonitoringModel
        }

        fun learnersMonitoringModel4(): SequenceMonitoringModel {
            val learnersMonitoringModel: SequenceMonitoringModel = SequenceMonitoringModel(
                ExecutionContext.FaceToFace,
                DashboardPhaseState.STOPPED,
                DashboardPhaseState.IN_PROGRESS,
                mutableListOf()
            )
            val learners = mutableListOf(
                LearnerMonitoringModel(
                    1,
                    "I've submitted my answer",
                    LearnerStateOnPhase.ACTIVITY_TERMINATED,
                    sequenceMonitoringModel = learnersMonitoringModel
                ),
                LearnerMonitoringModel(
                    2,
                    "B - I've not submitted my answer",
                    LearnerStateOnPhase.ACTIVITY_NOT_TERMINATED,
                    LearnerStateOnPhase.ACTIVITY_NOT_TERMINATED,
                    sequenceMonitoringModel = learnersMonitoringModel
                ),
                LearnerMonitoringModel(
                    5,
                    "A - I've not submitted my answer",
                    LearnerStateOnPhase.ACTIVITY_NOT_TERMINATED,
                    LearnerStateOnPhase.ACTIVITY_NOT_TERMINATED,
                    sequenceMonitoringModel = learnersMonitoringModel
                ),
                LearnerMonitoringModel(
                    3,
                    "I've finished both",
                    LearnerStateOnPhase.ACTIVITY_TERMINATED,
                    LearnerStateOnPhase.ACTIVITY_TERMINATED,
                    sequenceMonitoringModel = learnersMonitoringModel
                ),
                LearnerMonitoringModel(
                    4,
                    "B - I've didn't submit but i've evaluate",
                    LearnerStateOnPhase.ACTIVITY_NOT_TERMINATED,
                    LearnerStateOnPhase.ACTIVITY_TERMINATED,
                    sequenceMonitoringModel = learnersMonitoringModel
                ),
                LearnerMonitoringModel(
                    6,
                    "A - I've didn't submit but i've evaluate",
                    LearnerStateOnPhase.ACTIVITY_NOT_TERMINATED,
                    LearnerStateOnPhase.ACTIVITY_TERMINATED,
                    sequenceMonitoringModel = learnersMonitoringModel
                ),
            )

            learnersMonitoringModel.setLearners(learners)

            return learnersMonitoringModel
        }

        fun learnersMonitoringModel5(): SequenceMonitoringModel {
            val learnersMonitoringModel: SequenceMonitoringModel = SequenceMonitoringModel(
                ExecutionContext.FaceToFace,
                DashboardPhaseState.STOPPED,
                DashboardPhaseState.STOPPED,
                mutableListOf()
            )

            val learners = mutableListOf(
                LearnerMonitoringModel(
                    1,
                    "Alice",
                    LearnerStateOnPhase.ACTIVITY_TERMINATED,
                    LearnerStateOnPhase.ACTIVITY_TERMINATED,
                    LearnerStateOnPhase.ACTIVITY_NOT_TERMINATED,
                    learnersMonitoringModel
                ),
                LearnerMonitoringModel(
                    3,
                    "Jean",
                    LearnerStateOnPhase.ACTIVITY_TERMINATED,
                    LearnerStateOnPhase.ACTIVITY_NOT_TERMINATED,
                    LearnerStateOnPhase.ACTIVITY_NOT_TERMINATED,
                    learnersMonitoringModel
                ),
                LearnerMonitoringModel(
                    3,
                    "Ines",
                    LearnerStateOnPhase.ACTIVITY_TERMINATED,
                    LearnerStateOnPhase.ACTIVITY_NOT_TERMINATED,
                    LearnerStateOnPhase.ACTIVITY_NOT_TERMINATED,
                    learnersMonitoringModel
                ),
                LearnerMonitoringModel(
                    4,
                    "Pierre",
                    LearnerStateOnPhase.ACTIVITY_NOT_TERMINATED,
                    LearnerStateOnPhase.ACTIVITY_TERMINATED,
                    LearnerStateOnPhase.ACTIVITY_NOT_TERMINATED,
                    learnersMonitoringModel
                ),
                LearnerMonitoringModel(
                    4,
                    "Marc",
                    LearnerStateOnPhase.ACTIVITY_NOT_TERMINATED,
                    LearnerStateOnPhase.ACTIVITY_TERMINATED,
                    LearnerStateOnPhase.ACTIVITY_NOT_TERMINATED,
                    learnersMonitoringModel
                ),
                LearnerMonitoringModel(
                    2,
                    "Bob",
                    LearnerStateOnPhase.ACTIVITY_NOT_TERMINATED,
                    LearnerStateOnPhase.ACTIVITY_NOT_TERMINATED,
                    LearnerStateOnPhase.ACTIVITY_NOT_TERMINATED,
                    learnersMonitoringModel
                ),
            )

            learnersMonitoringModel.setLearners(learners)

            return learnersMonitoringModel
        }

        fun learnersMonitoringModel6(): SequenceMonitoringModel {
            val sequenceMonitoringModel: SequenceMonitoringModel = SequenceMonitoringModel(
                ExecutionContext.FaceToFace,
                DashboardPhaseState.STOPPED,
                DashboardPhaseState.STOPPED,
                mutableListOf()
            )

            val learners = mutableListOf(
                LearnerMonitoringModel(
                    1,
                    "Bob",
                    LearnerStateOnPhase.ACTIVITY_TERMINATED,
                    LearnerStateOnPhase.ACTIVITY_TERMINATED,
                    LearnerStateOnPhase.ACTIVITY_NOT_TERMINATED,
                    sequenceMonitoringModel
                ),
                LearnerMonitoringModel(
                    3,
                    "Albert",
                    LearnerStateOnPhase.ACTIVITY_TERMINATED,
                    LearnerStateOnPhase.ACTIVITY_TERMINATED,
                    LearnerStateOnPhase.ACTIVITY_NOT_TERMINATED,
                    sequenceMonitoringModel
                ),
                LearnerMonitoringModel(
                    3,
                    "Charlie",
                    LearnerStateOnPhase.ACTIVITY_TERMINATED,
                    LearnerStateOnPhase.ACTIVITY_NOT_TERMINATED,
                    LearnerStateOnPhase.ACTIVITY_NOT_TERMINATED,
                    sequenceMonitoringModel
                ),
                LearnerMonitoringModel(
                    4,
                    "Pierre",
                    LearnerStateOnPhase.ACTIVITY_NOT_TERMINATED,
                    LearnerStateOnPhase.ACTIVITY_NOT_TERMINATED,
                    LearnerStateOnPhase.ACTIVITY_NOT_TERMINATED,
                    sequenceMonitoringModel
                ),
            )
            sequenceMonitoringModel.setLearners(learners)
            return sequenceMonitoringModel
        }

        ////End of function and class use for this test case////

        val situationList = listOf(
            TableAttendeesSituation(
                "Phase 1 in progress with two students and one have finished",
                learnersMonitoringModel1()
            ),
            TableAttendeesSituation(
                "Nothing is started",
                learnersMonitoringModel2()
            ),
            TableAttendeesSituation(
                "Remote sequence",
                learnersMonitoringModel3()
            ),
            TableAttendeesSituation(
                "Phase 1 stopped and phase 2 in progress with two students with one student who hasn't submitted his " +
                        "response",
                learnersMonitoringModel4()
            ),
            TableAttendeesSituation(
                "Result phase started",
                learnersMonitoringModel5()
            ),
            TableAttendeesSituation(
                "Test situation",
                learnersMonitoringModel6()
            ),
        )

        model.addAttribute("user", user)
        model["situationList"] = situationList

        return "player/assignment/sequence/components/dashboard/test-attendees-table"
    }

    fun <T> List<T>.getRandom() = this[Random.nextInt(0, size)]

    @GetMapping("/getTestUsers")
    fun getTestUser(
        authentication: Authentication,
        model: Model,
        @RequestParam nbUserRequested: Int? = null,
    ): String {
        val safeNbUser = nbUserRequested ?: 0
        var existingUsers: List<User> = emptyList()
        if (0 < safeNbUser) {

            val emailDomain = "fakeemail"
            val testPswd = "1234"

            existingUsers = userRepository.findUsersByEmailLike("%$emailDomain%")
            if (safeNbUser < existingUsers.size) {
                existingUsers = existingUsers.subList(0, safeNbUser)
            } else if (existingUsers.size < safeNbUser) {
                val newUsers: MutableList<User> = emptyList<User>().toMutableList()
                val firstNames: List<String> = listOf(
                    "Emma",
                    "Jade",
                    "Louise",
                    "Alice",
                    "Chloé",
                    "Lina",
                    "Léa",
                    "Rose",
                    "Anna",
                    "Inès",
                    "Lucas",
                    "Maël",
                    "Nolan",
                    "Arthur",
                    "Théo",
                    "Ethan",
                    "Noah",
                    "Mathis"
                )
                val lastNames = listOf(
                    "Martin", "Bernard", "Dubois", "Thomas", "Robert", "Richard", "Petit", "Durand", "Leroy", "Moreau"
                )

                val nbUserToCreate = safeNbUser - existingUsers.size

                for (i in 1..nbUserToCreate) {
                    val firstName = firstNames.getRandom()
                    val lastName = lastNames.getRandom()
                    val username = userService.generateUsername(firstName, lastName)

                    val newUser = User(
                        firstName = firstName,
                        lastName = lastName,
                        username = username,
                        plainTextPassword = testPswd,
                        email = "$username@$emailDomain.com",
                        source = UserSource.ELAASTIC,
                    )

                    newUser.addRole(Role("STUDENT_ROLE"))
                    userService.addUser((newUser))
                    newUsers.add(newUser)
                }

                existingUsers = existingUsers + newUsers
            }
        }

        model["user"] = authentication.principal as User
        model["newUsersString"] = existingUsers.joinToString("\n")
        model["nbNewUser"] = existingUsers.size

        return "test/get-test-user"
    }
}
