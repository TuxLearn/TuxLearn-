package com.example.util

import com.example.data.model.MemoryRecallOutcome
import com.example.data.model.MemoryStrength
import com.example.data.model.StudiedTopic

object MemoryTestQuestionHelper {

    data class TopicQnA(
        val question: String,
        val answer: String,
        val explanation: String
    )

    /**
     * Resolves an active recall question, concise answer, and short explanation for a studied topic.
     * Ensures questions specifically challenge the student ("Without looking at your notes, ...").
     */
    fun getQuestionForTopic(subject: String, topic: String): TopicQnA {
        val lowerTopic = topic.lowercase().trim()
        val lowerSub = subject.lowercase().trim()

        return when {
            // Biology: Cell and Mitochondria (Exact requirement example)
            lowerTopic.contains("mitochondri") || (lowerTopic.contains("cell") && (lowerTopic.contains("energy") || lowerTopic.contains("organelle") || lowerTopic.contains("respirat"))) -> {
                TopicQnA(
                    question = "Without looking at your notes, what is the main function of mitochondria?",
                    answer = "Generate ATP (adenosine triphosphate) through cellular respiration.",
                    explanation = "Mitochondria are known as the powerhouses of the cell. Their primary function is to generate most of the chemical energy (ATP) needed by the cell through aerobic respiration, specifically via the citric acid cycle (Krebs cycle) and oxidative phosphorylation across the inner mitochondrial membrane."
                )
            }
            lowerTopic.contains("respiration") || lowerTopic.contains("krebs") || lowerTopic.contains("atp") -> {
                TopicQnA(
                    question = "Without looking at your notes, what are the four stages of cellular respiration and where does glycolysis occur?",
                    answer = "Glycolysis (cytosol), Pyruvate oxidation, Krebs cycle (matrix), and Oxidative phosphorylation (inner membrane).",
                    explanation = "Glycolysis takes place in the cytoplasm/cytosol. Pyruvate enters the mitochondrial matrix for conversion to Acetyl-CoA and the Krebs cycle. Finally, the electron transport chain and ATP synthase drive oxidative phosphorylation along the inner membrane cristae."
                )
            }
            lowerTopic.contains("mitosis") || lowerTopic.contains("cell division") -> {
                TopicQnA(
                    question = "Without looking at your notes, what are the four stages of mitosis in order, and what separates during anaphase?",
                    answer = "Prophase, Metaphase, Anaphase, Telophase (PMAT). Sister chromatids separate in anaphase.",
                    explanation = "Mitosis proceeds through PMAT: Prophase (chromatin condenses), Metaphase (chromosomes align at equator), Anaphase (spindle fibers pull sister chromatids to opposite poles), and Telophase (nuclear envelopes reform), followed by cytokinesis."
                )
            }
            lowerTopic.contains("photosynthesis") || lowerTopic.contains("chloroplast") -> {
                TopicQnA(
                    question = "Without looking at your notes, what are the primary inputs and outputs of the light-dependent reactions vs the Calvin cycle?",
                    answer = "Light reactions use H₂O and sunlight to produce O₂, ATP, and NADPH. The Calvin cycle uses CO₂, ATP, and NADPH to make glucose.",
                    explanation = "Light reactions occur in the thylakoid membranes, splitting water and releasing oxygen while synthesizing energy carriers. The Calvin cycle occurs in the stroma, fixing carbon dioxide into high-energy sugars."
                )
            }
            lowerTopic.contains("dna") || lowerTopic.contains("replication") || lowerTopic.contains("transcription") -> {
                TopicQnA(
                    question = "Without looking at your notes, what are the complementary base pairing rules in DNA and mRNA?",
                    answer = "In DNA: Adenine (A) pairs with Thymine (T), Guanine (G) pairs with Cytosine (C). In RNA, Uracil (U) replaces Thymine.",
                    explanation = "DNA replication pairs A-T (2 hydrogen bonds) and G-C (3 hydrogen bonds). During transcription to RNA, Adenine on the DNA template strand pairs with Uracil in the synthesized mRNA."
                )
            }

            // Mathematics: Integration by Parts & Calculus
            lowerTopic.contains("integration by parts") || lowerTopic.contains("by parts") -> {
                TopicQnA(
                    question = "Without looking at your notes, state the Integration by Parts formula and explain the LIATE priority rule.",
                    answer = "∫ u dv = u·v - ∫ v du (LIATE: Logarithmic, Inverse trig, Algebraic, Trig, Exponential).",
                    explanation = "Derived from the product rule: ∫ u dv = u·v - ∫ v du. Choose 'u' by prioritizing functions that become simpler upon differentiation: Logarithmic, Inverse trigonometric, Algebraic, Trigonometric, and Exponential."
                )
            }
            lowerTopic.contains("derivative") || lowerTopic.contains("chain rule") || lowerTopic.contains("product rule") -> {
                TopicQnA(
                    question = "Without looking at your notes, what is the chain rule formula for differentiating composite function f(g(x))?",
                    answer = "d/dx [f(g(x))] = f'(g(x)) · g'(x)",
                    explanation = "The chain rule states that the derivative of a composite function is the derivative of the outer function evaluated at the inner function, multiplied by the derivative of the inner function."
                )
            }
            lowerTopic.contains("quadratic") || lowerTopic.contains("roots") -> {
                TopicQnA(
                    question = "Without looking at your notes, what is the quadratic formula and what does the discriminant (b² - 4ac) indicate?",
                    answer = "x = (-b ± √(b² - 4ac)) / (2a). Discriminant > 0: two real roots; = 0: one repeated root; < 0: two complex roots.",
                    explanation = "The quadratic formula solves ax² + bx + c = 0. The discriminant b² - 4ac reveals the nature of the roots: positive yields two distinct real solutions, zero yields one unique real root, and negative yields complex conjugate roots."
                )
            }
            lowerTopic.contains("probability") || lowerTopic.contains("bayes") -> {
                TopicQnA(
                    question = "Without looking at your notes, what is Bayes' Theorem formula for updating prior probabilities?",
                    answer = "P(A|B) = [P(B|A) · P(A)] / P(B)",
                    explanation = "Bayes' Theorem calculates the posterior probability P(A|B) using the likelihood P(B|A), the prior probability P(A), and the marginal probability of the evidence P(B)."
                )
            }

            // Physics: Faraday's Law, Motion, Momentum
            lowerTopic.contains("faraday") || lowerTopic.contains("induction") || lowerTopic.contains("flux") -> {
                TopicQnA(
                    question = "Without looking at your notes, what does Faraday's Law state and what does the negative sign signify?",
                    answer = "EMF = -dΦ/dt. The negative sign represents Lenz's Law (opposing the flux change).",
                    explanation = "Faraday's Law states that the magnitude of induced electromotive force equals the rate of change of magnetic flux through a circuit. Lenz's Law (the negative sign) dictates that the induced current always creates a magnetic field that opposes the original change in flux."
                )
            }
            lowerTopic.contains("newton") || lowerTopic.contains("motion") || lowerTopic.contains("momentum") -> {
                TopicQnA(
                    question = "Without looking at your notes, state Newton's Second Law in terms of momentum rather than just F = m·a.",
                    answer = "F_net = dp/dt (Net force equals the time rate of change of linear momentum).",
                    explanation = "Newton originally defined force as the time rate of change of momentum: F_net = dp/dt = d(mv)/dt. For systems with constant mass, this simplifies to m(dv/dt) = m·a, but the momentum form generalizes to variable-mass systems like rockets."
                )
            }
            lowerTopic.contains("energy") || lowerTopic.contains("conservation of energy") -> {
                TopicQnA(
                    question = "Without looking at your notes, state the Law of Conservation of Mechanical Energy and the conditions required for it to hold.",
                    answer = "E_total = Kinetic Energy + Potential Energy = constant, provided only conservative forces do work.",
                    explanation = "In an isolated system where all internal forces are conservative (like gravity or ideal springs) and non-conservative forces (like friction or air resistance) do zero net work, total mechanical energy remains strictly constant."
                )
            }
            lowerTopic.contains("ohm") || lowerTopic.contains("circuit") || lowerTopic.contains("kirchhoff") -> {
                TopicQnA(
                    question = "Without looking at your notes, state Ohm's Law and Kirchhoff's Current Law (junction rule).",
                    answer = "V = I · R. Kirchhoff's Current Law: The sum of currents entering a junction equals the sum leaving (conservation of charge).",
                    explanation = "Ohm's Law relates potential difference V to current I and resistance R. Kirchhoff's Junction Law is a direct consequence of the conservation of electric charge: charge cannot accumulate indefinitely at an infinitesimal node."
                )
            }

            // Chemistry: Equilibrium, Periodic Trends, pH
            lowerTopic.contains("equilibrium") || lowerTopic.contains("le chatelier") -> {
                TopicQnA(
                    question = "Without looking at your notes, state Le Chatelier's Principle and how increasing temperature affects an exothermic reaction.",
                    answer = "Equilibrium shifts to counteract applied stress. For exothermic reactions, higher temperature shifts equilibrium left toward reactants.",
                    explanation = "Le Chatelier's Principle states that a chemical system at equilibrium responds to perturbations by shifting in a direction that opposes the disturbance. Because exothermic reactions release heat, adding heat acts like adding a product, shifting equilibrium left."
                )
            }
            lowerTopic.contains("periodic") || lowerTopic.contains("electronegativity") || lowerTopic.contains("trend") -> {
                TopicQnA(
                    question = "Without looking at your notes, how does electronegativity change across a period and down a group?",
                    answer = "Electronegativity increases across a period (left to right) and decreases down a group (top to bottom).",
                    explanation = "Across a period, increased positive nuclear charge pulls valence electrons more tightly without significant additional shielding. Down a group, added electron shells increase atomic radius and electron shielding, weakening nuclear attraction."
                )
            }
            lowerTopic.contains("acid") || lowerTopic.contains("base") || lowerTopic.contains("ph") -> {
                TopicQnA(
                    question = "Without looking at your notes, what is the mathematical definition of pH and what is the pH of a neutral aqueous solution at 25°C?",
                    answer = "pH = -log₁₀[H⁺]. Neutral water at 25°C has a pH of 7.0.",
                    explanation = "pH represents the negative logarithm of hydrogen/hydronium ion concentration. In pure water at 25°C, [H⁺] = 1.0 × 10⁻⁷ M, resulting in -log(10⁻⁷) = 7.0."
                )
            }

            // Computer Science: Trees, Sorting, Big-O
            lowerTopic.contains("tree") || lowerTopic.contains("bst") || lowerTopic.contains("binary search tree") -> {
                TopicQnA(
                    question = "Without looking at your notes, what is the key invariant property of a Binary Search Tree (BST)?",
                    answer = "For every node N: all keys in Left Subtree < N.key < all keys in Right Subtree.",
                    explanation = "In a binary search tree, every node satisfies the BST invariant: all elements in the left branch are strictly smaller, and all elements in the right branch are strictly larger. This property enables binary search with O(log n) average time complexity."
                )
            }
            lowerTopic.contains("quicksort") || lowerTopic.contains("quick sort") || lowerTopic.contains("sorting") -> {
                TopicQnA(
                    question = "Without looking at your notes, what is the average and worst-case time complexity of QuickSort, and what causes the worst case?",
                    answer = "Average: O(n log n). Worst-case: O(n²) when the pivot consistently partitions into 0 and n-1 elements.",
                    explanation = "QuickSort uses recursive partitioning. On average with balanced pivots, recursion depth is log n, taking O(n log n). If an extreme minimum or maximum is repeatedly chosen as the pivot on sorted data, the recursion tree becomes a degenerate line of depth n, taking O(n²)."
                )
            }
            lowerTopic.contains("graph") || lowerTopic.contains("bfs") || lowerTopic.contains("dfs") -> {
                TopicQnA(
                    question = "Without looking at your notes, what data structure does BFS use versus DFS, and which finds shortest unweighted paths?",
                    answer = "BFS uses a Queue (FIFO) and finds shortest unweighted paths. DFS uses a Stack (LIFO) or recursion.",
                    explanation = "Breadth-First Search traverses level by level using a First-In-First-Out Queue, guaranteeing the fewest edge transitions to reach any node in an unweighted graph. Depth-First Search traverses deeply down paths using a Stack or system call stack."
                )
            }

            // General Subject Smart Fallbacks
            lowerSub.contains("bio") -> {
                TopicQnA(
                    question = "Without looking at your notes, what is the fundamental biological mechanism, structural components, and cellular role of $topic?",
                    answer = "Core biological function and mechanism of $topic.",
                    explanation = "Key biological principles of $topic: Identify the molecular structures, enzymatic processes, cellular location, and how it sustains homeostasis."
                )
            }
            lowerSub.contains("phys") -> {
                TopicQnA(
                    question = "Without looking at your notes, what is the governing physical law, core equation, and fundamental conservation principle behind $topic?",
                    answer = "Governing physics principles and formula of $topic.",
                    explanation = "Core physical concepts of $topic: State the primary formulas, identify all variables and units, and apply conservation of energy, momentum, or charge."
                )
            }
            lowerSub.contains("chem") -> {
                TopicQnA(
                    question = "Without looking at your notes, explain the molecular behavior, reaction mechanism, and thermodynamic factors governing $topic.",
                    answer = "Chemical mechanisms, bonds, and reactions of $topic.",
                    explanation = "Key chemical concepts of $topic: Examine electron behavior, molecular geometry, bond types, reaction kinetics, and equilibrium states."
                )
            }
            lowerSub.contains("math") -> {
                TopicQnA(
                    question = "Without looking at your notes, state the fundamental theorem, required conditions, and step-by-step procedure for $topic.",
                    answer = "Mathematical definition, formula, and proof steps for $topic.",
                    explanation = "Essential mathematics of $topic: Verify hypotheses, apply the primary formula, and understand algebraic or geometric interpretations."
                )
            }
            lowerSub.contains("computer") || lowerSub.contains("cs") || lowerSub.contains("code") -> {
                TopicQnA(
                    question = "Without looking at your notes, what is the core algorithm/structure of $topic, its time/space complexity, and main tradeoff?",
                    answer = "Algorithmic logic and Big-O characteristics of $topic.",
                    explanation = "Core computer science principles of $topic: Analyze time complexity (best, average, worst), memory space, and practical implementation constraints."
                )
            }
            else -> {
                TopicQnA(
                    question = "Without looking at your notes, what is the core concept, key definition or formula, and main real-world application of $topic?",
                    answer = "Core concepts and fundamental definitions of $topic.",
                    explanation = "Active recall summary for $topic: Explain the primary definition in your own words, outline governing principles, and connect to practical exam scenarios."
                )
            }
        }
    }

    /**
     * Calculates the new memory strength, updated priority score, next test schedule, and XP reward
     * based on user's recall outcome.
     *
     * Strict Requirement:
     * - "Topics marked 'I Forgot' should appear more frequently in future Memory Tests."
     * - We boost priority score significantly (+25) and shorten the test interval (e.g. 2 hours) so they rise to the top.
     */
    fun evaluateRecallOutcome(
        currentTopic: StudiedTopic,
        outcome: MemoryRecallOutcome,
        nowMillis: Long = System.currentTimeMillis()
    ): RecallEvaluation {
        return when (outcome) {
            MemoryRecallOutcome.REMEMBERED -> {
                RecallEvaluation(
                    newMemoryStrength = MemoryStrength.STRONG.label,
                    newPriorityScore = (currentTopic.priorityScore - 10).coerceAtLeast(1),
                    nextScheduledEpochMillis = nowMillis + 3 * 24 * 3600 * 1000L, // 3 days
                    xpEarned = 25,
                    newForgotCount = currentTopic.forgotCount,
                    newRememberedCount = currentTopic.rememberedCount + 1,
                    feedbackMessage = "Excellent recall! Memory strength is now Strong. +25 XP"
                )
            }
            MemoryRecallOutcome.PARTIALLY_REMEMBERED -> {
                RecallEvaluation(
                    newMemoryStrength = MemoryStrength.MEDIUM.label,
                    newPriorityScore = (currentTopic.priorityScore + 5).coerceIn(10, 40),
                    nextScheduledEpochMillis = nowMillis + 24 * 3600 * 1000L, // 1 day
                    xpEarned = 20,
                    newForgotCount = currentTopic.forgotCount,
                    newRememberedCount = currentTopic.rememberedCount,
                    feedbackMessage = "Good effort! Memory strength marked as Medium. +20 XP"
                )
            }
            MemoryRecallOutcome.FORGOT -> {
                RecallEvaluation(
                    newMemoryStrength = MemoryStrength.NEEDS_REVISION.label,
                    // Higher priority score ensures it appears at the very top and more frequently in future tests
                    newPriorityScore = currentTopic.priorityScore + 25,
                    nextScheduledEpochMillis = nowMillis + 2 * 3600 * 1000L, // 2 hours later (high frequency)
                    xpEarned = 15,
                    newForgotCount = currentTopic.forgotCount + 1,
                    newRememberedCount = currentTopic.rememberedCount,
                    feedbackMessage = "Concept flagged for high-frequency revision! +15 XP for practicing recall."
                )
            }
        }
    }

    data class RecallEvaluation(
        val newMemoryStrength: String,
        val newPriorityScore: Int,
        val nextScheduledEpochMillis: Long,
        val xpEarned: Int,
        val newForgotCount: Int,
        val newRememberedCount: Int,
        val feedbackMessage: String
    )
}
