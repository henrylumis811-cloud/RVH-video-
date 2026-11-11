package com.rvh.video.data.local

import androidx.room.TypeConverter
import com.rvh.video.data.model.VideoCategory

class VideoCategoryConverters {
    @TypeConverter
    fun fromCategory(category: VideoCategory?): String? = category?.name

    @TypeConverter
    fun toCategory(value: String?): VideoCategory? = value?.let { VideoCategory.valueOf(it) }
}
