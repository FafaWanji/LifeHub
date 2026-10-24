package com.example.lifeorganizer.habits.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HabitQuickParseTest {
    private val mon = 1; private val tue = 2; private val wed = 4; private val thu = 8
    private val fri = 16; private val sat = 32; private val sun = 64

    @Test fun daily() {
        assertEquals(QuickHabit("Sport", HabitSchedule.EVERY_DAY, 7 * 60), HabitQuickParse.parse("Sport täglich 7 Uhr"))
        assertEquals(QuickHabit("Lesen", HabitSchedule.EVERY_DAY, null), HabitQuickParse.parse("Lesen jeden Tag"))
        assertEquals(QuickHabit("Read", HabitSchedule.EVERY_DAY, 21 * 60 + 30), HabitQuickParse.parse("Read at 9:30 pm every day"))
        assertEquals(QuickHabit("Meditate", HabitSchedule.EVERY_DAY, null), HabitQuickParse.parse("Meditate daily"))
    }

    @Test fun weekdayLists() {
        assertEquals(QuickHabit("Gym", mon or wed or fri, 18 * 60), HabitQuickParse.parse("Gym Mo Mi Fr 18:00"))
        assertEquals(QuickHabit("Run", mon or thu, null), HabitQuickParse.parse("Run every mon and thu"))
        assertEquals(QuickHabit("Joggen", mon, null), HabitQuickParse.parse("Joggen montags"))
        assertEquals(QuickHabit("Schwimmen", tue or thu, null), HabitQuickParse.parse("Schwimmen dienstags und donnerstags"))
        assertEquals(QuickHabit("Gitarre", wed, 19 * 60), HabitQuickParse.parse("Gitarre jeden Mittwoch um 19 Uhr"))
        assertEquals(QuickHabit("Yoga", sat or sun, null), HabitQuickParse.parse("Yoga Sa, So"))
    }

    @Test fun rangesAndGroups() {
        assertEquals(QuickHabit("Vokabeln", mon or tue or wed or thu or fri, null), HabitQuickParse.parse("Vokabeln Mo-Fr"))
        assertEquals(QuickHabit("Party", fri or sat or sun or mon, null), HabitQuickParse.parse("Party Fr bis Mo"))
        assertEquals(QuickHabit("Pendeln", mon or tue or wed or thu or fri, null), HabitQuickParse.parse("Pendeln werktags"))
        assertEquals(QuickHabit("Putzen", sat or sun, 10 * 60), HabitQuickParse.parse("Putzen am Wochenende 10 Uhr"))
        assertEquals(QuickHabit("Walk", sat or sun, null), HabitQuickParse.parse("Walk on weekends"))
    }

    @Test fun deadlinesStayTasks() {
        assertNull(HabitQuickParse.parse("Bericht Fr"))
        assertNull(HabitQuickParse.parse("Zahnarzt am Freitag 14 Uhr"))
        assertNull(HabitQuickParse.parse("Aufgabe 5 morgen"))
        assertNull(HabitQuickParse.parse("Mach das immer so"))
        assertNull(HabitQuickParse.parse("täglich"))
    }

    @Test fun habitsTabMakesEveryLineAHabit() {
        assertEquals(QuickHabit("Wasser trinken", HabitSchedule.EVERY_DAY, null), HabitQuickParse.parse("Wasser trinken", repeatNeeded = false))
        assertEquals(QuickHabit("Vitamine", HabitSchedule.EVERY_DAY, 8 * 60), HabitQuickParse.parse("Vitamine 8 Uhr", repeatNeeded = false))
    }
}
