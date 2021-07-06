package com.github.layoric.servicestackidea.services

import com.github.layoric.servicestackidea.MyBundle
import com.intellij.openapi.project.Project

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
