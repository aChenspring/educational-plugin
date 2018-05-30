package com.jetbrains.edu.learning.courseFormat.ext

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task

val Course.isToPush get(): PushStatus {
  return PushStatus.NOTHING
}

val Lesson.isToPush get(): PushStatus {
  return PushStatus.NOTHING
}

val Task.isToPush get(): PushStatus {
  return PushStatus.NOTHING
}

val TaskFile.isToPush get(): PushStatus {
  return PushStatus.NOTHING
}

enum class PushStatus{
  ALL, INFO, CONTENT, NOTHING
}