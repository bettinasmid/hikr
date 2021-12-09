package hu.bme.aut.android.hiketracker.service

import android.content.Context
import android.location.Location

interface NewLocationHandler{
    suspend fun onNewLocation(location: Location)

    suspend fun finish()
}