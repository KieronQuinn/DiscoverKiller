package com.kieronquinn.app.discoverkiller.repositories

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface RootRepository {

    suspend fun isRooted(): Boolean

}

class RootRepositoryImpl: RootRepository {

    override suspend fun isRooted(): Boolean {
        return withContext(Dispatchers.IO){
            Shell.cmd("whoami").exec().out.firstOrNull() == "root"
        }
    }

}