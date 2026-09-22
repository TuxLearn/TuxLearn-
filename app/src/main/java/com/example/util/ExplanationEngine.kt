package com.example.util

import com.example.data.model.StudyExplanation
import java.util.Locale

/**
 * Local AI & Knowledge Engine for the "Ask & Explain" feature.
 * Provides student-friendly explanations with:
 * 1. Direct Answer
 * 2. Easy Explanation
 * 3. A Simple Example
 * 4. Important Exam Point
 * 5. One Quick Recall Question & Answer
 */
object ExplanationEngine {

    val SUPPORTED_SUBJECTS = listOf(
        "Physics",
        "Chemistry",
        "Biology",
        "Mathematics",
        "Computer Science",
        "English",
        "Other"
    )

    data class SamplePrompt(
        val subject: String,
        val prompt: String
    )

    val SAMPLE_PROMPTS = listOf(
        SamplePrompt("Physics", "What is Newton's Third Law?"),
        SamplePrompt("Physics", "Explain Ohm's Law and its formula."),
        SamplePrompt("Physics", "Why does refraction happen when light enters water?"),
        SamplePrompt("Chemistry", "What is the difference between covalent and ionic bonds?"),
        SamplePrompt("Chemistry", "How does Le Chatelier's Principle predict equilibrium shifts?"),
        SamplePrompt("Chemistry", "Explain exothermic vs endothermic reactions with examples."),
        SamplePrompt("Biology", "How does Photosynthesis convert sunlight into chemical energy?"),
        SamplePrompt("Biology", "What is the main difference between Mitosis and Meiosis?"),
        SamplePrompt("Biology", "Why is the Mitochondria called the powerhouse of the cell?"),
        SamplePrompt("Mathematics", "Explain the Pythagorean Theorem and when to use it."),
        SamplePrompt("Mathematics", "What does a Derivative actually measure?"),
        SamplePrompt("Mathematics", "How do you calculate probability for independent events?"),
        SamplePrompt("Computer Science", "What is Binary Search and why is its time complexity O(log n)?"),
        SamplePrompt("Computer Science", "Explain the 4 Pillars of Object-Oriented Programming (OOP)."),
        SamplePrompt("Computer Science", "What is the difference between a Stack and a Queue?"),
        SamplePrompt("English", "What is the difference between a Metaphor and a Simile?"),
        SamplePrompt("English", "How do I change Active Voice into Passive Voice?"),
        SamplePrompt("English", "What makes a strong thesis statement in an essay?"),
        SamplePrompt("Other", "What is Opportunity Cost in decision making?"),
        SamplePrompt("Other", "What are the core steps of the Scientific Method?")
    )

    /**
     * Generates a complete 5-part explanation for the student's question.
     */
    fun explainQuestion(question: String, selectedSubject: String): StudyExplanation {
        val cleanQ = question.trim()
        val normalized = cleanQ.lowercase(Locale.ROOT)

        // 1. Check curated knowledge base for exact/keyword matches
        val curated = findCuratedExplanation(normalized, selectedSubject, cleanQ)
        if (curated != null) {
            return curated
        }

        // 2. Dynamic Algorithmic Synthesis for arbitrary questions
        return synthesizeDynamicExplanation(cleanQ, selectedSubject)
    }

    private fun findCuratedExplanation(
        normalized: String,
        subject: String,
        originalQuestion: String
    ): StudyExplanation? {

        // PHYSICS
        if (normalized.contains("newton") && (normalized.contains("third") || normalized.contains("3rd") || normalized.contains("action"))) {
            return StudyExplanation(
                question = originalQuestion,
                subject = "Physics",
                directAnswer = "Newton's Third Law states that for every action, there is an equal and opposite reaction force acting on two different interacting bodies simultaneously.",
                easyExplanation = "Whenever Object A pushes on Object B, Object B pushes back on Object A with the exact same magnitude of force, but in the exact opposite direction. These forces always exist in matched pairs (called action-reaction pairs) and they never act on the same object, which is why they do not cancel each other out.",
                example = "When you jump off a skateboard onto a sidewalk, your feet push the skateboard backward (action force), and the skateboard pushes you forward through the air (reaction force).",
                examPoint = "Key marking trap: Action and reaction forces NEVER act on the same body! Always specify the two separate objects involved to receive full marks.",
                quickRecallQuestion = "If Object A exerts a 50 N force north on Object B, what force does Object B exert on Object A?",
                quickRecallAnswer = "50 N directed south (equal magnitude, opposite direction)."
            )
        }

        if (normalized.contains("ohm") || normalized.contains("v = ir") || normalized.contains("resistance")) {
            return StudyExplanation(
                question = originalQuestion,
                subject = "Physics",
                directAnswer = "Ohm's Law states that electric current (I) flowing through a conductor between two points is directly proportional to voltage (V) across the points, provided temperature and other physical conditions remain constant (V = I × R).",
                easyExplanation = "Think of voltage as electrical pressure pushing water through a pipe, current as the rate water flows, and resistance as a narrowing in the pipe. If you double the pushing pressure (voltage), twice as much current will flow, unless the pipe resists it more (resistance).",
                example = "If a flashlight bulb is connected to a 6V battery and has a resistance of 3 Ohms (Ω), the current flowing through it is I = V / R = 6 / 3 = 2 Amperes.",
                examPoint = "Condition requirement: Examiners will deduct marks if you forget to mention 'at constant temperature'. Standard SI units: Voltage in Volts (V), Current in Amperes (A), Resistance in Ohms (Ω).",
                quickRecallQuestion = "If voltage across a 10 Ω resistor is 20 V, what is the current?",
                quickRecallAnswer = "2 Amperes (I = V / R = 20 / 10 = 2 A)."
            )
        }

        if (normalized.contains("refraction") || normalized.contains("snell") || normalized.contains("bend") && normalized.contains("light")) {
            return StudyExplanation(
                question = originalQuestion,
                subject = "Physics",
                directAnswer = "Refraction is the bending of a light wave when it passes obliquely from one optical medium into another of different optical density, caused by a change in wave speed.",
                easyExplanation = "Light travels at different speeds in different materials (faster in air, slower in glass or water). When a light ray enters water at an angle, one side of the wave slows down before the other, causing the entire beam to pivot and bend toward the normal line.",
                example = "A straight straw placed inside a clear glass of water appears bent or severed at the water's surface because light from under the water refracts away from the normal into your eyes.",
                examPoint = "Remember the rule: Rare to Dense = Bends TOWARDS the normal line. Dense to Rare = Bends AWAY from the normal line. Snell's Law: n₁ sin(θ₁) = n₂ sin(θ₂).",
                quickRecallQuestion = "Does light bend towards or away from the normal when entering glass from air?",
                quickRecallAnswer = "Towards the normal (air is optically rarer, glass is denser)."
            )
        }

        if (normalized.contains("conservation of energy") || (normalized.contains("energy") && normalized.contains("create") && normalized.contains("destroy"))) {
            return StudyExplanation(
                question = originalQuestion,
                subject = "Physics",
                directAnswer = "The Law of Conservation of Energy states that energy can neither be created nor destroyed; it can only transform from one form into another. The total energy of an isolated system remains constant.",
                easyExplanation = "You cannot summon new energy out of thin air, and spent energy never vanishes into nothingness. When energy seems to 'disappear' (like a moving car braking to a stop), it has simply converted into heat, sound, or internal energy.",
                example = "A rollercoaster cart at the highest peak has maximum gravitational potential energy (mgh). As it plunges downwards, potential energy converts smoothly into kinetic speed energy (½mv²).",
                examPoint = "Exam formula: Initial Total Mechanical Energy = Final Total Mechanical Energy (E_initial = E_final). State that friction transforms mechanical energy into thermal energy.",
                quickRecallQuestion = "What happens to the potential energy of an object when it falls in a vacuum?",
                quickRecallAnswer = "It is converted entirely into kinetic energy (total mechanical energy stays constant)."
            )
        }

        // CHEMISTRY
        if (normalized.contains("covalent") || normalized.contains("ionic") || (normalized.contains("bond") && normalized.contains("difference"))) {
            return StudyExplanation(
                question = originalQuestion,
                subject = "Chemistry",
                directAnswer = "Ionic bonds form through the complete transfer of valence electrons between metals and nonmetals, whereas covalent bonds form through the mutual sharing of electron pairs between nonmetals.",
                easyExplanation = "In ionic bonding, one atom donates electrons and becomes a positive ion (cation), while another atom accepts them to become a negative ion (anion), holding together like opposite magnetic poles. In covalent bonding, neither atom wants to give up electrons entirely, so they hold hands and share them to fill their outer shells.",
                example = "Table salt (NaCl) is ionic: Sodium gives 1 electron to Chlorine. Water (H₂O) is covalent: Oxygen shares pairs of electrons with two Hydrogen atoms.",
                examPoint = "Comparison questions: Mention melting points (Ionic compounds have high melting points due to strong electrostatic lattice; Covalent molecular compounds have lower melting points due to weak intermolecular forces).",
                quickRecallQuestion = "Which type of bonding occurs between two nonmetal atoms?",
                quickRecallAnswer = "Covalent bonding (electrons are shared)."
            )
        }

        if (normalized.contains("le chatelier") || normalized.contains("chatelier") || normalized.contains("equilibrium shift")) {
            return StudyExplanation(
                question = originalQuestion,
                subject = "Chemistry",
                directAnswer = "Le Chatelier's Principle states that if a dynamic equilibrium system is subjected to a disturbance (change in concentration, temperature, or pressure), the system will adjust to counteract the imposed change and re-establish equilibrium.",
                easyExplanation = "Equilibrium acts like a stubborn balance scale. If you dump extra heat or reactants on one side, the reaction rushes in the direction that burns up that excess. If you remove something, the reaction shifts to produce more of what was lost.",
                example = "In the Haber process for making ammonia (N₂ + 3H₂ ⇌ 2NH₃ + heat), increasing pressure shifts the reaction to the right because 4 moles of gas are compressed into 2 moles of gas.",
                examPoint = "Temperature rules: Increasing temperature favors the endothermic direction. Decreasing temperature favors the exothermic direction. Adding a catalyst DOES NOT shift equilibrium; it only speeds up reaching equilibrium.",
                quickRecallQuestion = "Does adding an inert catalyst shift the equilibrium position of a reaction?",
                quickRecallAnswer = "No, a catalyst increases the rate of both forward and reverse reactions equally without changing position."
            )
        }

        if (normalized.contains("exothermic") || normalized.contains("endothermic")) {
            return StudyExplanation(
                question = originalQuestion,
                subject = "Chemistry",
                directAnswer = "An exothermic reaction releases thermal energy to its surroundings (ΔH is negative), causing temperature to rise, while an endothermic reaction absorbs thermal energy from its surroundings (ΔH is positive), causing temperature to drop.",
                easyExplanation = "Think of 'Exo' as 'Exit' (heat exits the reaction beaker into the room, feeling hot to your hands). Think of 'Endo' as 'Enter' (heat enters into the chemical bonds from the surroundings, making the flask feel icy cold).",
                example = "Exothermic: A camp campfire burning wood or hand warmers. Endothermic: An instant ice pack snapped on an athlete's sprained ankle, or baking a cake.",
                examPoint = "Sign convention: Always write the sign of enthalpy change! Exothermic has ΔH < 0 (negative). Endothermic has ΔH > 0 (positive). Energy diagram: Exothermic products are lower than reactants.",
                quickRecallQuestion = "What is the sign of ΔH for an exothermic reaction?",
                quickRecallAnswer = "Negative (ΔH < 0), because energy is released to the surroundings."
            )
        }

        // BIOLOGY
        if (normalized.contains("photosynthesis")) {
            return StudyExplanation(
                question = originalQuestion,
                subject = "Biology",
                directAnswer = "Photosynthesis is the biochemical process by which autotrophic plants convert light energy, carbon dioxide (CO₂), and water (H₂O) into glucose (chemical energy) and oxygen gas (O₂), occurring inside chloroplasts.",
                easyExplanation = "Plants act as solar-powered food factories. Chlorophyll pigments trap sunlight rays. They split water molecules, release oxygen as a byproduct for us to breathe, and combine hydrogen with carbon dioxide to manufacture sugary glucose for plant growth.",
                example = "Green tree leaves turning sunlight on a bright summer afternoon into starch stored inside roots, stems, and fruits.",
                examPoint = "Chemical Equation to memorize: 6CO₂ + 6H₂O + Light Energy → C₆H₁₂O₆ + 6O₂. Specify that light-dependent reactions happen in thylakoids, while the Calvin cycle happens in the stroma.",
                quickRecallQuestion = "What green organelle in plant cells carries out photosynthesis?",
                quickRecallAnswer = "Chloroplast (containing chlorophyll)."
            )
        }

        if (normalized.contains("mitosis") || normalized.contains("meiosis")) {
            return StudyExplanation(
                question = originalQuestion,
                subject = "Biology",
                directAnswer = "Mitosis is somatic cell division producing two genetically identical diploid daughter cells for growth and repair, while meiosis is a two-step germ cell division producing four genetically diverse haploid gametes for sexual reproduction.",
                easyExplanation = "Mitosis: 'My-toes-is' (repairs your toes/skin by cloning exact copies). Meiosis: 'Me-one-is' (makes sperm or egg sex cells with half the chromosomes so parents can combine traits).",
                example = "Mitosis heals a skin scrape by replicating identical skin cells. Meiosis occurs in human ovaries or testes to generate sperm and egg cells with 23 chromosomes each.",
                examPoint = "Chromosome Count: Mitosis preserves chromosome number (2n → 2n). Meiosis halves chromosome number (2n → n) and introduces genetic variation via crossing over in Prophase I.",
                quickRecallQuestion = "How many daughter cells are produced at the end of Meiosis?",
                quickRecallAnswer = "Four genetically distinct haploid daughter cells."
            )
        }

        if (normalized.contains("mitochondria") || normalized.contains("powerhouse")) {
            return StudyExplanation(
                question = originalQuestion,
                subject = "Biology",
                directAnswer = "Mitochondria are double-membraned cellular organelles that generate most of the cell's chemical energy supply in the form of Adenosine Triphosphate (ATP) via aerobic cellular respiration.",
                easyExplanation = "Mitochondria take broken-down food nutrients (glucose derivatives) and oxygen, then run them through an internal molecular generator called the Electron Transport Chain to recharge thousands of cellular 'batteries' called ATP.",
                example = "Human muscle cells contain thousands of mitochondria because they require continuous, high-volume energy output during running or heavy lifting.",
                examPoint = "Exam terms: State 'ATP synthesis through oxidative phosphorylation'. Note that mitochondria contain their own circular DNA (mtDNA) and double membrane with folded cristae.",
                quickRecallQuestion = "What high-energy molecule is synthesized by mitochondria to fuel cellular work?",
                quickRecallAnswer = "ATP (Adenosine Triphosphate)."
            )
        }

        // MATHEMATICS
        if (normalized.contains("pythagor") || normalized.contains("a^2 + b^2") || normalized.contains("hypotenuse")) {
            return StudyExplanation(
                question = originalQuestion,
                subject = "Mathematics",
                directAnswer = "The Pythagorean Theorem states that in any right-angled triangle, the square of the hypotenuse is equal to the sum of the squares of the other two perpendicular sides: a² + b² = c².",
                easyExplanation = "If you build an actual square on side 'a' and an actual square on side 'b', their combined surface areas will fit perfectly inside the square built on the longest diagonal side 'c' (the hypotenuse).",
                example = "For a triangle with legs 3 cm and 4 cm: 3² + 4² = 9 + 16 = 25. Therefore, the hypotenuse c = √25 = 5 cm (the classic 3-4-5 right triangle).",
                examPoint = "Prerequisite reminder: This theorem ONLY applies to 90° right-angled triangles! If the triangle is non-right angled, you must use the Law of Cosines (c² = a² + b² - 2ab cos C).",
                quickRecallQuestion = "What is the length of the hypotenuse if the two legs are 6 cm and 8 cm?",
                quickRecallAnswer = "10 cm (6² + 8² = 36 + 64 = 100, √100 = 10)."
            )
        }

        if (normalized.contains("derivative") || normalized.contains("calculus") && normalized.contains("slope")) {
            return StudyExplanation(
                question = originalQuestion,
                subject = "Mathematics",
                directAnswer = "A derivative measures the instantaneous rate of change of a function with respect to an independent variable, geometrically representing the exact slope of the tangent line to the curve at any point.",
                easyExplanation = "Average speed is distance divided by whole hours. But the speedometer in your car shows how fast you are moving at this exact split-second. The derivative is that mathematical speedometer for any curve or process.",
                example = "If a car's distance after t seconds is given by s(t) = 5t², the derivative ds/dt = 10t gives the instantaneous velocity. At t = 3 seconds, velocity is 30 m/s.",
                examPoint = "Power rule: For f(x) = xⁿ, the derivative is f'(x) = n · xⁿ⁻¹. Don't forget that the derivative of any standalone constant is 0!",
                quickRecallQuestion = "What is the derivative of f(x) = 4x³?",
                quickRecallAnswer = "12x² (using the power rule: 4 × 3x²)."
            )
        }

        if (normalized.contains("probability") && (normalized.contains("independent") || normalized.contains("events"))) {
            return StudyExplanation(
                question = originalQuestion,
                subject = "Mathematics",
                directAnswer = "Two events A and B are independent if the occurrence of one event has no effect whatsoever on the probability of the other occurring: P(A ∩ B) = P(A) × P(B).",
                easyExplanation = "If flipping a coin lands on Heads, it does not change the chances of rolling a 6 on a die. They have zero memory of each other. You simply multiply their individual probabilities together.",
                example = "The chance of rolling an odd number on a die is 1/2. The chance of flipping Heads is 1/2. The probability of both occurring together is 1/2 × 1/2 = 1/4 (25%).",
                examPoint = "Formula distinction: Independent events use Multiplication Rule P(A and B) = P(A) × P(B). Mutually exclusive events CANNOT happen together: P(A and B) = 0.",
                quickRecallQuestion = "If P(A) = 0.4 and P(B) = 0.5 for independent events, what is P(A and B)?",
                quickRecallAnswer = "0.20 (0.4 × 0.5 = 0.20 or 20%)."
            )
        }

        // COMPUTER SCIENCE
        if (normalized.contains("binary search")) {
            return StudyExplanation(
                question = originalQuestion,
                subject = "Computer Science",
                directAnswer = "Binary Search is an efficient divide-and-conquer algorithm for finding a target value within a SORTED array in O(log n) logarithmic time by repeatedly halving the search interval.",
                easyExplanation = "Imagine looking for a word in a 1,000-page printed dictionary. You don't read every page from page 1. You open to the middle (page 500). If your word comes after, you discard the entire first half and repeat the process on the remainder.",
                example = "Searching through an array of 1,000,000 elements requires at most 20 comparisons with Binary Search (since 2²⁰ ≈ 1,048,576), compared to up to 1,000,000 checks in Linear Search.",
                examPoint = "Mandatory prerequisite: The input array MUST be sorted before executing Binary Search! If unsorted, you must sort first or use linear search.",
                quickRecallQuestion = "What is the worst-case time complexity of Binary Search?",
                quickRecallAnswer = "O(log n) logarithmic time."
            )
        }

        if (normalized.contains("oop") || (normalized.contains("object oriented") && normalized.contains("pillar"))) {
            return StudyExplanation(
                question = originalQuestion,
                subject = "Computer Science",
                directAnswer = "The 4 fundamental pillars of Object-Oriented Programming (OOP) are Encapsulation (data bundling and hiding), Abstraction (hiding complexity), Inheritance (code reuse via hierarchies), and Polymorphism (many forms/overriding).",
                easyExplanation = "Encapsulation keeps internal variables safe inside a private capsule. Abstraction gives you a simple steering wheel without needing to see the engine valves. Inheritance lets a SportsCar reuse Car features. Polymorphism lets different objects respond to the same command (e.g., calling .drive()).",
                example = "A 'Dog' and 'Cat' class both inherit from an 'Animal' base class, but each overrides the makeSound() method to emit 'Bark' and 'Meow' respectively (Polymorphism).",
                examPoint = "Exam definition accuracy: Clearly differentiate Abstraction (what it does - interfaces/abstract classes) from Encapsulation (how data is protected - access modifiers private/protected).",
                quickRecallQuestion = "Which OOP pillar restricts direct access to an object's internal fields using getters and setters?",
                quickRecallAnswer = "Encapsulation."
            )
        }

        if (normalized.contains("stack") && normalized.contains("queue")) {
            return StudyExplanation(
                question = originalQuestion,
                subject = "Computer Science",
                directAnswer = "A Stack is a LIFO (Last-In, First-Out) data structure where the last inserted element is removed first, whereas a Queue is a FIFO (First-In, First-Out) data structure where elements are served in arrival order.",
                easyExplanation = "A Stack is like a stack of dinner plates in a cafeteria: you take plates off the top and put new ones on top. A Queue is like a ticket line at the cinema: the first person who arrived buys their ticket and leaves first.",
                example = "Stack: Browser Back button history or Undo (Ctrl+Z) in word processors. Queue: Print job spooler or keyboard keystroke buffer.",
                examPoint = "Core Operations: Stack uses push() and pop() at one end (top). Queue uses enqueue() at the rear and dequeue() from the front. Both perform in O(1) constant time.",
                quickRecallQuestion = "Does a Queue operate on LIFO or FIFO principle?",
                quickRecallAnswer = "FIFO (First-In, First-Out)."
            )
        }

        // ENGLISH
        if (normalized.contains("metaphor") || normalized.contains("simile")) {
            return StudyExplanation(
                question = originalQuestion,
                subject = "English",
                directAnswer = "A simile compares two distinct things using connecting words such as 'like' or 'as', whereas a metaphor directly asserts that one thing IS another to imply symbolic similarity.",
                easyExplanation = "Similes compare things side-by-side with an obvious connector: 'He fought like a lion.' Metaphors take it a step further and transform identity: 'He was a lion in the battlefield.'",
                example = "Simile: 'Her smile was as radiant as the morning sun.' Metaphor: 'Her smile was the morning sun breaking through our dark day.'",
                examPoint = "Analysis tip in English exams: Identify the vehicle and the tenor! Explain the emotional effect or connotation the metaphor evokes rather than just stating what it means literally.",
                quickRecallQuestion = "Identify the literary device: 'Life is a rollercoaster with twists and turns.'",
                quickRecallAnswer = "Metaphor (it states life IS a rollercoaster without using 'like' or 'as')."
            )
        }

        if (normalized.contains("active") && normalized.contains("passive")) {
            return StudyExplanation(
                question = originalQuestion,
                subject = "English",
                directAnswer = "In the active voice, the subject of the sentence performs the action upon the object (Subject + Verb + Object). In the passive voice, the object receives the action, often with a form of 'to be' and past participle (Object + was/were + Verb-ed + by Subject).",
                easyExplanation = "Active voice is direct, punchy, and confident: the doer comes first. Passive voice makes the sentence indirect or shifts focus to the result, sometimes leaving out who did it entirely.",
                example = "Active: 'Shakespeare wrote Hamlet.' Passive: 'Hamlet was written by Shakespeare.'",
                examPoint = "Writing tip: Academic and persuasive essays prefer active voice for clarity and conciseness. Use passive voice intentionally when the actor is unknown, irrelevant, or in objective scientific lab reports ('The solution was heated...').",
                quickRecallQuestion = "Convert to active voice: 'The marathon was won by Maya.'",
                quickRecallAnswer = "'Maya won the marathon.'"
            )
        }

        return null
    }

    private fun synthesizeDynamicExplanation(
        question: String,
        selectedSubject: String
    ): StudyExplanation {
        // Extract key topic phrase by removing question words
        val cleaned = question
            .replace(Regex("(?i)^(what is|what are|how do|how does|why does|why do|explain|tell me about|describe|define)\\s+"), "")
            .replace(Regex("[?!.]+$"), "")
            .trim()
            .ifBlank { "this concept" }

        val capitalizedTopic = cleaned.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

        val subjectContext = when (selectedSubject) {
            "Physics" -> "in physics, governed by fundamental physical laws, forces, and mathematical relationships"
            "Chemistry" -> "in chemistry, concerning atomic structure, bonding behavior, and thermodynamic energy exchanges"
            "Biology" -> "in biology, vital to living organisms, cellular mechanisms, and physiological adaptation"
            "Mathematics" -> "in mathematics, forming an analytical structure proven through rigorous definitions and properties"
            "Computer Science" -> "in computer science, optimizing computational efficiency, data integrity, and algorithmic logic"
            "English" -> "in English and literary analysis, shaping tone, rhetorical clarity, and effective reader communication"
            else -> "in academic study, building fundamental conceptual understanding and critical reasoning"
        }

        val direct = "$capitalizedTopic is a core fundamental principle $subjectContext. In direct terms, it describes how specific conditions and rules interact to yield predictable, measurable outcomes."

        val easy = """
1. Core Idea: When exploring $capitalizedTopic, break it down into simple inputs and outputs. It provides the mechanism for why this phenomenon occurs consistently under standard conditions.
2. Step-by-Step: First, identify the interacting elements. Second, observe how energy, forces, logic, or meaning flow between them. Finally, observe the balanced result.
3. Student Takeaway: Rather than memorizing raw formulas or definitions, picture the moving components and how each piece depends on the previous one.
        """.trimIndent()

        val example = "Think of $capitalizedTopic like a well-tuned mechanism or everyday system. For instance, when you apply an input or change a parameter, the whole system adapts proportionately to maintain balance and produce a dependable result."

        val examPoint = "Examiner Tip for $selectedSubject: Clearly state the primary definition, use correct academic terminology, and specify units or underlying assumptions. Marks are awarded for explaining the underlying reason, not just stating the final output."

        val recallQ = "In your own words, what is the single most important rule or definition that defines $capitalizedTopic?"
        val recallA = "$capitalizedTopic relies on consistent underlying principles $subjectContext; always identify the core conditions and their direct impact."

        return StudyExplanation(
            question = question,
            subject = selectedSubject,
            directAnswer = direct,
            easyExplanation = easy,
            example = example,
            examPoint = examPoint,
            quickRecallQuestion = recallQ,
            quickRecallAnswer = recallA
        )
    }
}
