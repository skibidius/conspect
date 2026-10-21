package ru.konspekt.app.data

import java.util.concurrent.TimeUnit

object SeedData {
    val subjects = listOf(
        Subject(
            id = "math",
            name = "Математический анализ",
            shortName = "Матан",
            teacher = "доц. Соколова",
            room = "402",
            nextClass = "Сегодня, 10:15",
            tone = SubjectTone.SAGE,
            icon = SubjectIcon.FUNCTIONS
        ),
        Subject(
            id = "physics",
            name = "Физика",
            shortName = "Физика",
            teacher = "проф. Лебедев",
            room = "215",
            nextClass = "Сегодня, 12:10",
            tone = SubjectTone.SKY,
            icon = SubjectIcon.SCIENCE
        ),
        Subject(
            id = "cs",
            name = "Информатика",
            shortName = "Информатика",
            teacher = "доц. Ким",
            room = "Лаб. 3",
            nextClass = "Завтра, 09:00",
            tone = SubjectTone.LILAC,
            icon = SubjectIcon.TERMINAL
        ),
        Subject(
            id = "history",
            name = "История",
            shortName = "История",
            teacher = "проф. Орлова",
            room = "118",
            nextClass = "Чт, 14:30",
            tone = SubjectTone.AMBER,
            icon = SubjectIcon.LANDMARK
        ),
        Subject(
            id = "english",
            name = "Английский язык",
            shortName = "Английский",
            teacher = "ст. преп. Райт",
            room = "207",
            nextClass = "Пт, 11:20",
            tone = SubjectTone.CLAY,
            icon = SubjectIcon.LANGUAGE
        )
    )

    val schedule = listOf(
        ScheduleItem("10:15", "11:40", "math", "Производная сложной функции", "402", true),
        ScheduleItem("12:10", "13:35", "physics", "Законы Ньютона", "215"),
        ScheduleItem("15:40", "17:05", "english", "Present Perfect", "207")
    )

    private fun ago(minutes: Long): Long =
        System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(minutes)

    val mathLatex = """
        \documentclass[11pt]{article}
        \usepackage[T2A]{fontenc}
        \usepackage[utf8]{inputenc}
        \usepackage[russian]{babel}
        \usepackage{amsmath, amssymb}
        \usepackage{geometry}
        \geometry{margin=2.4cm}

        \title{Производная сложной функции}
        \author{Conspect}
        \date{\today}

        \begin{document}
        \maketitle

        \section{Определение}
        Производная функции в точке характеризует мгновенную скорость
        изменения функции:
        \[
            f'(x) = \lim_{\Delta x \to 0}
            \frac{f(x + \Delta x) - f(x)}{\Delta x}.
        \]

        \section{Правило цепочки}
        Если функция g дифференцируема в точке x, а функция f — в точке g(x), то
        \[
            \left(f(g(x))\right)' = f'(g(x)) \cdot g'(x).
        \]

        \section{Пример}
        Для функции y = sin(3x² + 1):
        \[
            y' = \cos(3x^2 + 1) \cdot 6x.
        \]
        \end{document}
    """.trimIndent()

    val notes = listOf(
        Note(
            id = "note-derivative",
            subjectId = "math",
            title = "Производная сложной функции",
            subtitle = "Правило цепочки и практика",
            summary = "Производная сложной функции вычисляется по правилу цепочки: сначала находим производную внешней функции, затем умножаем её на производную внутренней.",
            createdAt = ago(38),
            dateLabel = "Сегодня",
            timeLabel = "10:42",
            durationMinutes = 42,
            wordCount = 486,
            keyTerms = listOf("Производная", "Правило цепочки", "Внутренняя функция", "Внешняя функция"),
            sections = listOf(
                NoteSection(
                    title = "Определение производной",
                    kind = SectionKind.DEFINITION,
                    body = "Производная функции в точке характеризует мгновенную скорость изменения функции. Если функция дифференцируема в точке x, то её производная равна пределу отношения приращения функции к приращению аргумента.",
                    formula = """f'(x) = \lim_{\Delta x \to 0} \frac{f(x + \Delta x) - f(x)}{\Delta x}""",
                    formulaLabel = "Предел отношения приращений"
                ),
                NoteSection(
                    title = "Правило цепочки",
                    kind = SectionKind.THEOREM,
                    body = "Пусть функция g дифференцируема в точке x, а функция f — в точке g(x). Тогда сложная функция f(g(x)) дифференцируема, и её производная равна произведению производной внешней функции на производную внутренней.",
                    formula = """\left(f(g(x))\right)' = f'(g(x)) \cdot g'(x)""",
                    formulaLabel = "Основная формула"
                ),
                NoteSection(
                    title = "Разбор примера",
                    kind = SectionKind.EXAMPLE,
                    body = "Найдём производную функции y = sin(3x² + 1). Внешняя функция — синус, внутренняя — квадратичная функция.",
                    formula = """y' = \cos(3x^2 + 1) \cdot 6x""",
                    formulaLabel = "Ответ",
                    bullets = listOf(
                        "Определяем внешнюю функцию: f(u) = sin u.",
                        "Находим производную внешней функции: cos u.",
                        "Умножаем на производную внутренней функции: 6x."
                    )
                ),
                NoteSection(
                    title = "Что запомнить",
                    kind = SectionKind.SUMMARY,
                    body = "",
                    bullets = listOf(
                        "Правило цепочки работает для любого числа вложенных функций.",
                        "Сначала дифференцируем снаружи, затем внутри.",
                        "Не забывайте про область определения и допустимые значения."
                    )
                )
            ),
            skipped = listOf(
                SkippedFragment(
                    time = "12:18",
                    quote = "В среду в 16:00 будет консультация в 402-й аудитории. Приходите, если остались вопросы по пределам.",
                    reason = "Организационное"
                ),
                SkippedFragment(
                    time = "24:05",
                    quote = "Небольшое отступление про поездку на конференцию в Казань и про то, как я потерял доклад.",
                    reason = "Не по теме"
                )
            ),
            latexSource = mathLatex
        ),
        Note(
            id = "note-newton",
            subjectId = "physics",
            title = "Законы Ньютона",
            subtitle = "Динамика и инерциальные системы",
            summary = "Три закона Ньютона связывают силу, массу и ускорение. Вместе они образуют основу классической механики и позволяют описывать движение тел.",
            createdAt = ago(60L * 22),
            dateLabel = "Вчера",
            timeLabel = "12:18",
            durationMinutes = 38,
            wordCount = 364,
            keyTerms = listOf("Инерция", "Сила", "Ускорение", "Инерциальная система"),
            sections = listOf(
                NoteSection(
                    title = "Первый закон",
                    kind = SectionKind.DEFINITION,
                    body = "Существуют такие системы отсчёта, относительно которых тело сохраняет состояние покоя или равномерного прямолинейного движения, если на него не действуют силы или их действие скомпенсировано."
                ),
                NoteSection(
                    title = "Второй закон",
                    kind = SectionKind.THEOREM,
                    body = "Ускорение тела прямо пропорционально равнодействующей сил и обратно пропорционально массе тела.",
                    formula = """\vec{F} = m\vec{a}""",
                    formulaLabel = "Второй закон Ньютона"
                ),
                NoteSection(
                    title = "Третий закон",
                    kind = SectionKind.THEOREM,
                    body = "Тела действуют друг на друга с силами, равными по модулю и противоположными по направлению.",
                    formula = """\vec{F}_{12} = -\vec{F}_{21}""",
                    formulaLabel = "Действие и противодействие"
                )
            ),
            skipped = listOf(
                SkippedFragment(
                    time = "08:40",
                    quote = "Сначала пару минут про то, как правильно оформлять лабораторные работы.",
                    reason = "Организационное"
                )
            ),
            latexSource = """
                \documentclass{article}
                \usepackage{amsmath}
                \begin{document}
                \section{Второй закон Ньютона}
                \[ \vec{F} = m\vec{a} \]
                \section{Третий закон Ньютона}
                \[ \vec{F}_{12} = -\vec{F}_{21} \]
                \end{document}
            """.trimIndent()
        ),
        Note(
            id = "note-sorting",
            subjectId = "cs",
            title = "Алгоритмы сортировки",
            subtitle = "Сложность и сравнение",
            summary = "Сортировка сравнением не может работать быстрее O(n log n) в общем случае. Быстрая сортировка выигрывает на практике, а сортировка слиянием даёт стабильную оценку.",
            createdAt = ago(60L * 27),
            dateLabel = "Вчера",
            timeLabel = "16:05",
            durationMinutes = 51,
            wordCount = 512,
            keyTerms = listOf("Quick sort", "Merge sort", "O(n log n)", "Стабильность"),
            sections = listOf(
                NoteSection(
                    title = "Нижняя оценка",
                    kind = SectionKind.DEFINITION,
                    body = "Любой алгоритм сортировки, основанный на попарных сравнениях, в худшем случае выполняет не меньше Ω(n log n) сравнений.",
                    formula = """T(n) = \Omega(n \log n)""",
                    formulaLabel = "Нижняя граница"
                ),
                NoteSection(
                    title = "Быстрая сортировка",
                    kind = SectionKind.TEXT,
                    body = "Выбираем опорный элемент, разбиваем массив на две части и рекурсивно сортируем каждую. Средняя сложность — O(n log n), худшая — O(n²).",
                    bullets = listOf(
                        "На практике обычно быстрее сортировки слиянием.",
                        "Не является стабильной.",
                        "Требует аккуратного выбора опорного элемента."
                    )
                ),
                NoteSection(
                    title = "Сортировка слиянием",
                    kind = SectionKind.TEXT,
                    body = "Делим массив пополам, сортируем половины и сливаем их. Сложность всегда O(n log n), но нужен дополнительный массив.",
                    formula = """T(n) = 2T(n/2) + O(n) = O(n \log n)""",
                    formulaLabel = "Рекуррентное соотношение"
                )
            ),
            latexSource = """
                \documentclass{article}
                \usepackage{amsmath}
                \begin{document}
                \section{Сложность сортировки}
                \[ T(n) = O(n \log n) \]
                \end{document}
            """.trimIndent()
        ),
        Note(
            id = "note-present-perfect",
            subjectId = "english",
            title = "Present Perfect",
            subtitle = "Связь прошлого с настоящим",
            summary = "Present Perfect описывает действие, результат которого важен сейчас. Время не указывается точно, а акцент делается на результате.",
            createdAt = ago(60L * 44),
            dateLabel = "18 октября",
            timeLabel = "11:24",
            durationMinutes = 30,
            wordCount = 289,
            keyTerms = listOf("have/has + V3", "since", "for", "already"),
            sections = listOf(
                NoteSection(
                    title = "Формула",
                    kind = SectionKind.DEFINITION,
                    body = "Утверждение строится по схеме: подлежащее + have/has + причастие прошедшего времени (V3).",
                    formula = """\text{Subject} + \text{have/has} + V_3""",
                    formulaLabel = "Структура"
                ),
                NoteSection(
                    title = "Когда используем",
                    kind = SectionKind.TEXT,
                    body = "Когда действие завершилось, но его результат связан с настоящим, либо когда период времени ещё не закончился.",
                    bullets = listOf(
                        "I have finished my essay. — Я закончил эссе (результат сейчас).",
                        "She has lived here since 2020. — Она живёт здесь с 2020 года.",
                        "We have already seen this film."
                    )
                )
            )
        ),
        Note(
            id = "note-hundred-years",
            subjectId = "history",
            title = "Столетняя война",
            subtitle = "Причины, этапы и итоги",
            summary = "Столетняя война (1337–1453) была конфликтом династий и торговых интересов. Она ускорила формирование централизованных государств и изменила военное дело.",
            createdAt = ago(60L * 68),
            dateLabel = "17 октября",
            timeLabel = "15:12",
            durationMinutes = 44,
            wordCount = 433,
            keyTerms = listOf("Плантагенеты", "Валуа", "Жанна д’Арк", "Бургундия"),
            sections = listOf(
                NoteSection(
                    title = "Причины",
                    kind = SectionKind.TEXT,
                    body = "Спор о французском престоле между династиями Валуа и Плантагенетов, а также борьба за Фландрию и контроль над торговыми путями.",
                    bullets = listOf(
                        "Династический кризис после смерти Карла IV.",
                        "Экономические интересы в Фландрии.",
                        "Соперничество за влияние в Западной Европе."
                    )
                ),
                NoteSection(
                    title = "Перелом",
                    kind = SectionKind.TEXT,
                    body = "Появление Жанны д’Арк и снятие осады Орлеана в 1429 году изменили ход войны. После этого инициатива перешла к французской короне."
                ),
                NoteSection(
                    title = "Итоги",
                    kind = SectionKind.SUMMARY,
                    body = "Англия потеряла почти все владения во Франции, кроме Кале. Война способствовала укреплению центральной власти и развитию постоянной армии.",
                    bullets = listOf(
                        "Усиление французской монархии.",
                        "Упадок рыцарской конницы как основной силы.",
                        "Рост национального самосознания."
                    )
                )
            )
        ),
        Note(
            id = "note-limits",
            subjectId = "math",
            title = "Пределы и непрерывность",
            subtitle = "Свойства и точки разрыва",
            summary = "Предел описывает поведение функции вблизи точки. Непрерывность означает, что предел функции совпадает со значением функции в этой точке.",
            createdAt = ago(60L * 96),
            dateLabel = "15 октября",
            timeLabel = "10:46",
            durationMinutes = 36,
            wordCount = 341,
            keyTerms = listOf("Предел", "Непрерывность", "Разрыв", "Односторонний предел"),
            sections = listOf(
                NoteSection(
                    title = "Определение предела",
                    kind = SectionKind.DEFINITION,
                    body = "Число L называется пределом функции f в точке a, если значения функции можно сделать сколь угодно близкими к L, выбирая x достаточно близко к a.",
                    formula = """\lim_{x \to a} f(x) = L""",
                    formulaLabel = "Предел функции"
                ),
                NoteSection(
                    title = "Непрерывность",
                    kind = SectionKind.THEOREM,
                    body = "Функция непрерывна в точке a, если она определена в этой точке, предел существует и равен значению функции.",
                    formula = """\lim_{x \to a} f(x) = f(a)""",
                    formulaLabel = "Условие непрерывности"
                )
            ),
            skipped = listOf(
                SkippedFragment(
                    time = "18:30",
                    quote = "Рассказ про то, как важно вовремя сдавать домашние задания.",
                    reason = "Организационное"
                )
            )
        ),
        Note(
            id = "note-induction",
            subjectId = "physics",
            title = "Электромагнитная индукция",
            subtitle = "Поток, правило Ленца и ЭДС",
            summary = "Изменяющийся магнитный поток порождает ЭДС индукции. Направление индукционного тока определяется правилом Ленца и всегда противодействует изменению потока.",
            createdAt = ago(60L * 120),
            dateLabel = "14 октября",
            timeLabel = "12:22",
            durationMinutes = 40,
            wordCount = 312,
            keyTerms = listOf("Магнитный поток", "ЭДС", "Правило Ленца", "Контур"),
            sections = listOf(
                NoteSection(
                    title = "Закон Фарадея",
                    kind = SectionKind.THEOREM,
                    body = "ЭДС индукции в замкнутом контуре равна скорости изменения магнитного потока через поверхность, ограниченную контуром, взятой со знаком минус.",
                    formula = """\varepsilon = -\frac{d\Phi}{dt}""",
                    formulaLabel = "Закон электромагнитной индукции"
                ),
                NoteSection(
                    title = "Правило Ленца",
                    kind = SectionKind.TEXT,
                    body = "Индукционный ток направлен так, чтобы своим магнитным полем противодействовать изменению магнитного потока, вызвавшему этот ток."
                )
            )
        )
    )
}
