package com.example.server.seed;

import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Hardcoded mock data for development and testing.
 *
 * <p>Seeds the following course catalog:
 * <ul>
 *   <li>COMPSCI 121 — no prerequisites</li>
 *   <li>COMPSCI 187 — prerequisite: COMPSCI 121</li>
 *   <li>COMPSCI 198C — prerequisite: COMPSCI 121</li>
 *   <li>CICS 210 — prerequisite: COMPSCI 121</li>
 *   <li>MATH 131 — no prerequisites</li>
 *   <li>MATH 132 — prerequisite: MATH 131</li>
 *   <li>STATISTICS 315 — no prerequisites</li>
 *   <li>COMPSCI 220 — prerequisite: COMPSCI 187</li>
 *   <li>COMPSCI 230 — prerequisite: COMPSCI 187 AND COMPSCI 198C</li>
 *   <li>COMPSCI 240 — prerequisite: COMPSCI 187 AND MATH 132</li>
 *   <li>COMPSCI 250 — prerequisite: COMPSCI 187 AND MATH 132</li>
 *   <li>COMPSCI 311 — prerequisite: COMPSCI 187 AND COMPSCI 250</li>
 *   <li>COMPSCI 320 — prerequisite: COMPSCI 220</li>
 *   <li>COMPSCI 326 — prerequisite: COMPSCI 220</li>
 *   <li>COMPSCI 345 — prerequisite: CICS 210 OR COMPSCI 187</li>
 *   <li>COMPSCI 377 — prerequisite: COMPSCI 230</li>
 *   <li>COMPSCI 383 — prerequisite: (CICS 210 OR COMPSCI 187) AND (COMPSCI 240 OR STATISTICS 315)</li>
 *   <li>COMPSCI 453 — prerequisite: COMPSCI 230 OR COMPSCI 377</li>
 *   <li>COMPSCI 485 — prerequisite: COMPSCI 220 AND COMPSCI 240</li>
 *   <li>COMPSCI 514 — prerequisite: (COMPSCI 240 OR STATISTICS 315) AND COMPSCI 311</li>
 *   <li>COMPSCI 520 — prerequisite: COMPSCI 320 OR (COMPSCI 220 AND COMPSCI 326)</li>
 *   <li>COMPSCI 445 — prerequisite: (COMPSCI 220 OR COMPSCI 230) AND COMPSCI 311 AND COMPSCI 345</li>
 *   <li>COMPSCI 532 — prerequisite: COMPSCI 377 AND COMPSCI 445</li>
 * </ul>
 *
 * <p>Replace this class with a {@code WebCrawlerCourseDataProvider} that
 * implements {@link CourseDataProvider} to scrape live data from the
 * UMass Spire/course catalog.
 */
@Component
public class MockCourseDataProvider implements CourseDataProvider {

    @Override
    public List<UserDefinition> getUserDefinitions() {
        return List.of(
            new UserDefinition("student-001", "John Doe", "jdoe@umass.edu", null),
            new UserDefinition("student-002", "Jane Smith", "jsmith@umass.edu", null)
        );
    }

    @Override
    public List<CourseDefinition> getCourseDefinitions() {
        return List.of(

            // ── Introductory courses (no prerequisites) ─────────

            new CourseDefinition(
                "COMPSCI 121",
                "Introduction to Problem Solving with Computers",
                4,
                "An introductory course in computer science for non-CS majors.",
                null
            ),

            new CourseDefinition(
                "MATH 131",
                "Calculus I",
                4,
                "First course in calculus covering limits, derivatives, and integrals.",
                null
            ),

            new CourseDefinition(
                "MATH 132",
                "Calculus II",
                4,
                "Second course in calculus covering integration techniques and series.",
                RequirementDefinition.course("MATH 131")
            ),

            new CourseDefinition(
                "STATISTICS 315",
                "Statistics I",
                3,
                "Introduction to basic statistics, expected value, variance, distribution models.",
                null
            ),

            // ── Intermediate courses ────────────────────────────

            new CourseDefinition(
                "COMPSCI 187",
                "Programming with Data Structures",
                4,
                "Use, design, and analysis of data structures.",
                RequirementDefinition.course("COMPSCI 121")
            ),

            new CourseDefinition(
                "CICS 210",
                "Data Structures",
                4,
                "An introduction to the design, analysis, and implementation of data structures. "
                    + "This course teaches you how to build, test, debug, document, and evaluate objects "
                    + "that encapsulate data and their associated operations using programming constructs "
                    + "and data abstractions of a modern programming language. Concepts and techniques "
                    + "covered include linear and non-linear structures, recursive structures and algorithms, "
                    + "traversal algorithms, binary search trees, balanced trees, priority queues, union-find, "
                    + "hash tables, bloom filters, and graphs. Also covers run time analysis and Big-O notation.",
                RequirementDefinition.course("COMPSCI 121")
            ),

            new CourseDefinition(
                "COMPSCI 198C",
                "P-Intro/C Programming Language",
                1,
                "This practicum assumes general background and experience in computer programming "
                    + "(such as that provided by COMPSCI 121 or a similar introductory programming course) "
                    + "and some knowledge of data structures. Content will include basic C data types, "
                    + "declarations, expressions, statements, and functions; simple use of macros; some "
                    + "common library calls (such as formatted input/output); basic pointer manipulation "
                    + "using linked lists; and introduction to using standard tools (gcc and make). "
                    + "A required prerequisite for COMPSCI 230, effective Fall 2023.",
                RequirementDefinition.course("COMPSCI 121")
            ),

            // ── Upper-level courses ─────────────────────────────

            new CourseDefinition(
                "COMPSCI 220",
                "Programming Methodology",
                4,
                "Development of individual skills necessary for designing, implementing, testing and "
                    + "modifying larger programs, including: design strategies and patterns, using functional "
                    + "and object-oriented approaches, testing and program verification, code refactoring, "
                    + "interfacing with libraries. There will be significant programming and mid-term and "
                    + "final examinations.",
                RequirementDefinition.course("COMPSCI 187")
            ),

            // COMPSCI 230 requires: COMPSCI 187 AND COMPSCI 198C
            new CourseDefinition(
                "COMPSCI 230",
                "Computer Systems Principles",
                4,
                "Large-scale software systems like Google - deployed over a world-wide network of hundreds "
                    + "of thousands of computers - have become a part of our lives. The course begins with a "
                    + "discussion of C data representation, and moves up the stack from there to the features "
                    + "of modern architectures, assembly languages, and operating system services such as I/O, "
                    + "process, and synchronization.",
                RequirementDefinition.and(
                    RequirementDefinition.course("COMPSCI 187"),
                    RequirementDefinition.course("COMPSCI 198C")
                )
            ),

            // COMPSCI 240 requires: COMPSCI 187 AND MATH 132
            new CourseDefinition(
                "COMPSCI 240",
                "Reasoning Under Uncertainty",
                4,
                "Development of mathematical reasoning skills for problems that involve uncertainty. "
                    + "Topics include counting and probability, probability definitions, mean, variance, "
                    + "binomial distribution, discrete and continuous random variables, Markov and Chebyshev "
                    + "bounds, Laws of large numbers, and central limit theorem. Probabilistic reasoning covers "
                    + "conditional probability and odds, Bayes' Law, Markov Chains, and Bayesian Networks. "
                    + "Statistical topics such as estimation of parameters and linear regression, as time permits.",
                RequirementDefinition.and(
                    RequirementDefinition.course("COMPSCI 187"),
                    RequirementDefinition.course("MATH 132")
                )
            ),

            // COMPSCI 250 requires: COMPSCI 187 AND MATH 132
            new CourseDefinition(
                "COMPSCI 250",
                "Introduction To Computation",
                4,
                "Basic concepts of discrete mathematics useful to computer science: set theory, strings "
                    + "and formal languages, propositional and predicate calculus, relations and functions, "
                    + "basic number theory. Induction and recursion: interplay of inductive definition, "
                    + "inductive proof, and recursive algorithms. Graphs, trees, and search. Finite-state "
                    + "machines, regular languages, nondeterministic finite automata, Kleene's Theorem. "
                    + "Problem sets, 2 midterm exams, timed final.",
                RequirementDefinition.and(
                    RequirementDefinition.course("COMPSCI 187"),
                    RequirementDefinition.course("MATH 132")
                )
            ),

            // COMPSCI 311 requires: COMPSCI 187 AND COMPSCI 250
            // (PDF also lists MATH 455 as alternative to COMPSCI 250, omitted — not in catalog)
            new CourseDefinition(
                "COMPSCI 311",
                "Introduction to Algorithms",
                4,
                "This course introduces techniques to design algorithms such as divide and conquer, greedy, "
                    + "dynamic programming, and network flow. Students will learn to study the performance of "
                    + "various algorithms within a formal, mathematical framework, design very efficient algorithms "
                    + "for many kinds of problems, and recognize problems that currently do not have efficient "
                    + "algorithms (NP-Completeness). This course is required for the CS Major (BS) and counts as "
                    + "a CS Elective for the CS Major (BA).",
                RequirementDefinition.and(
                    RequirementDefinition.course("COMPSCI 187"),
                    RequirementDefinition.course("COMPSCI 250")
                )
            ),

            new CourseDefinition(
                "COMPSCI 320",
                "Software Engineering",
                4,
                "In this course, students learn and gain practical experience with software engineering "
                    + "principles and techniques. The practical experience centers on a semester-long team "
                    + "project in which a software development project is carried through all the stages of "
                    + "the software life cycle. Topics include requirements analysis, specification, design, "
                    + "abstraction, programming style, testing, maintenance, communication, teamwork, and "
                    + "software project management.",
                RequirementDefinition.course("COMPSCI 220")
            ),

            new CourseDefinition(
                "COMPSCI 326",
                "Web Programming",
                4,
                "The web is today's most important application platform. This course will cover a broad "
                    + "range of client-side web technologies, including HTTP, HTML5, CSS, and JavaScript, "
                    + "as well as server-side concepts including AJAX, JavaScript libraries, and web security. "
                    + "Students will construct a substantial dynamic web application based on the concepts "
                    + "and techniques presented during lectures and in readings.",
                RequirementDefinition.course("COMPSCI 220")
            ),

            // COMPSCI 345 requires: CICS 210 OR COMPSCI 187
            new CourseDefinition(
                "COMPSCI 345",
                "Pract & Appl of Data Management",
                3,
                "Computing has become data-driven, and databases are now at the heart of commercial "
                    + "applications. This course provides a comprehensive introduction to the use of data "
                    + "management systems within the context of various applications. The emphasis is on "
                    + "relational databases, though non-relational databases are also introduced. Topics "
                    + "include the relational data model, data retrieval, application-driven database design, "
                    + "schema refinement, implementation of basic transactions, and database security.",
                RequirementDefinition.or(
                    RequirementDefinition.course("CICS 210"),
                    RequirementDefinition.course("COMPSCI 187")
                )
            ),

            // COMPSCI 445 requires: (COMPSCI 220 OR COMPSCI 230) AND COMPSCI 311 AND COMPSCI 345
            new CourseDefinition(
                "COMPSCI 445",
                "Information Systems",
                3,
                "This course is an introduction to the efficient management of large-scale data. "
                    + "The course includes principles for representing information as structured data, "
                    + "query languages for analyzing and manipulating structured data, and core systems "
                    + "principles that enable efficient computation on large data sets. Classical relational "
                    + "database topics will be covered (data modeling, SQL, query optimization, concurrency "
                    + "control), as well as semi-structured data (XML, JSON), and distributed data processing "
                    + "paradigms (e.g. MapReduce and Spark).",
                RequirementDefinition.and(
                    RequirementDefinition.or(
                        RequirementDefinition.course("COMPSCI 220"),
                        RequirementDefinition.course("COMPSCI 230")
                    ),
                    RequirementDefinition.course("COMPSCI 311"),
                    RequirementDefinition.course("COMPSCI 345")
                )
            ),

            new CourseDefinition(
                "COMPSCI 377",
                "Operating Systems",
                4,
                "In this course we examine the important problems in operating system design and "
                    + "implementation. The operating system provides a well-known, convenient, and efficient "
                    + "interface between user programs and the bare hardware of the computer. Topics include "
                    + "process management (processes, threads, CPU scheduling, synchronization, and deadlock), "
                    + "memory management (segmentation, paging, swapping), file systems, and operating system "
                    + "support for distributed systems. Programming projects in C.",
                RequirementDefinition.course("COMPSCI 230")
            ),

            // COMPSCI 383 requires: (CICS 210 OR COMPSCI 187) AND (COMPSCI 240 OR STATISTICS 315)
            new CourseDefinition(
                "COMPSCI 383",
                "Artificial Intelligence",
                3,
                "This course aims to give students a high level understanding of the prominent AI topics "
                    + "employed in industry today. It covers machine learning, data structures, and programming "
                    + "fundamentals. Graph and tree data structures will be used in particular. Topics include "
                    + "an overview of supporting algorithms and examples of products powered by the technology. "
                    + "Students will need a fundamental understanding of data structures and programming fundamentals.",
                RequirementDefinition.and(
                    RequirementDefinition.or(
                        RequirementDefinition.course("CICS 210"),
                        RequirementDefinition.course("COMPSCI 187")
                    ),
                    RequirementDefinition.or(
                        RequirementDefinition.course("COMPSCI 240"),
                        RequirementDefinition.course("STATISTICS 315")
                    )
                )
            ),

            // COMPSCI 453 requires: COMPSCI 230 OR COMPSCI 377
            new CourseDefinition(
                "COMPSCI 453",
                "Computer Networks",
                3,
                "This course provides an introduction to fundamental concepts in the design and "
                    + "implementation of computer networks, their protocols, and applications with a "
                    + "particular emphasis on the Internet's TCP/IP protocol suite. Topics include network "
                    + "architectures, applications, network programming interfaces, transport, congestion, "
                    + "routing, data link protocols, addressing, local area networks, wireless networks, "
                    + "network security, and network management.",
                RequirementDefinition.or(
                    RequirementDefinition.course("COMPSCI 230"),
                    RequirementDefinition.course("COMPSCI 377")
                )
            ),

            // COMPSCI 485 requires: COMPSCI 220 AND COMPSCI 240
            // (PDF also lists LINGUIST 429B as alternative, omitted — not in catalog)
            new CourseDefinition(
                "COMPSCI 485",
                "Applications of NLP",
                3,
                "This course will introduce NLP methods and applications, such as text classification, "
                    + "sentiment analysis, machine translation, and other applications to identify and use "
                    + "the meaning of text. During the course, students will learn fundamental methods and "
                    + "algorithms for NLP, become familiar with key facts about human language, and complete "
                    + "a series of hands-on projects to use, implement, experiment with, and improve NLP tools.",
                RequirementDefinition.and(
                    RequirementDefinition.course("COMPSCI 220"),
                    RequirementDefinition.course("COMPSCI 240")
                )
            ),

            // COMPSCI 514 requires: (COMPSCI 240 OR STATISTICS 315) AND COMPSCI 311
            new CourseDefinition(
                "COMPSCI 514",
                "Algorithms for Data Science",
                3,
                "With the advent of social networks, ubiquitous sensors, and large-scale computational "
                    + "science, data scientists must deal with data that is massive in size, arrives at "
                    + "blinding speeds, and often must be processed within interactive or quasi-interactive "
                    + "time frames. This course studies the mathematical foundations of big data processing, "
                    + "developing algorithms and learning how to analyze them, and explores methods for "
                    + "sampling, sketching, and distributed processing of large scale databases, graphs, "
                    + "and data streams for scalable statistical description, querying, pattern mining, and learning.",
                RequirementDefinition.and(
                    RequirementDefinition.or(
                        RequirementDefinition.course("COMPSCI 240"),
                        RequirementDefinition.course("STATISTICS 315")
                    ),
                    RequirementDefinition.course("COMPSCI 311")
                )
            ),

            // COMPSCI 520 requires: COMPSCI 320 OR (COMPSCI 220 AND COMPSCI 326)
            new CourseDefinition(
                "COMPSCI 520",
                "Theory & Practice of Software Engineering",
                3,
                "Introduces students to the principal activities and state-of-the-art techniques involved "
                    + "in developing high-quality software systems. Topics include: AI for software engineering "
                    + "including automated testing, program repair and software verification; software engineering "
                    + "for AI like detecting and fixing fairness bugs in AI models; and how industry works.",
                RequirementDefinition.or(
                    RequirementDefinition.course("COMPSCI 320"),
                    RequirementDefinition.and(
                        RequirementDefinition.course("COMPSCI 220"),
                        RequirementDefinition.course("COMPSCI 326")
                    )
                )
            ),

            // COMPSCI 532 requires: COMPSCI 377 AND COMPSCI 445
            new CourseDefinition(
                "COMPSCI 532",
                "Systems for Data Science",
                3,
                "In this course, students will learn the fundamentals behind large-scale systems in the "
                    + "context of data science. We will cover the issues involved in scaling up (to many "
                    + "processors) and out (to many nodes) parallelism in order to perform fast analyses on "
                    + "large datasets. Topics include locality and data representation, concurrency, distributed "
                    + "databases and systems, performance analysis and understanding, and existing and emerging "
                    + "data science platforms including MapReduce-Hadoop, Spark, and more.",
                RequirementDefinition.and(
                    RequirementDefinition.course("COMPSCI 377"),
                    RequirementDefinition.course("COMPSCI 445")
                )
            )
        );
    }

    @Override
    public List<CompletedCourseDefinition> getCompletedCourseDefinitions() {
        return List.of(
            // Student "student-001" has completed enough for COMPSCI 220
            new CompletedCourseDefinition("student-001", "COMPSCI 121", "A",  "Fall 2024"),
            new CompletedCourseDefinition("student-001", "COMPSCI 187", "B+", "Spring 2025"),
            new CompletedCourseDefinition("student-001", "MATH 131",   "A-", "Fall 2024"),

            // Student "student-002" is missing COMPSCI 187
            new CompletedCourseDefinition("student-002", "COMPSCI 121", "B",  "Fall 2024"),
            new CompletedCourseDefinition("student-002", "MATH 132",   "B+", "Spring 2025")
        );
    }
}
