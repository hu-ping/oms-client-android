/*
 * Copyright (C) 2018 Intel Corporation
 * SPDX-License-Identifier: Apache-2.0
 */
package oms.conference;

import static oms.base.CheckCondition.DCHECK;
import static oms.conference.JsonUtils.getObj;
import static oms.conference.JsonUtils.getString;

import oms.base.AudioCodecParameters;
import oms.base.MediaCodecs.AudioCodec;
import oms.base.MediaCodecs.VideoCodec;
import oms.base.VideoCodecParameters;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Capabilities for subscribing a RemoteStream, which indicates the video or/and audio options
 * that ConferenceClient may use to subscribe a RemoteStream. Subscribing a RemoteStream with the
 * options that beyonds its SubscriptionCapabilities may cause failure.
 */
public class SubscriptionCapabilities {

    public final AudioSubscriptionCapabilities audioSubscriptionCapabilities;
    public final VideoSubscriptionCapabilities videoSubscriptionCapabilities;

    SubscriptionCapabilities(JSONObject mediaInfo) throws JSONException {
        DCHECK(mediaInfo);

        JSONObject audio = JsonUtils.getObj(mediaInfo, "audio");
        audioSubscriptionCapabilities = audio == null ? null
                : new AudioSubscriptionCapabilities(audio);

        JSONObject video = JsonUtils.getObj(mediaInfo, "video");
        videoSubscriptionCapabilities = video == null ? null
                : new VideoSubscriptionCapabilities(video);
    }

    /**
     * Audio capabilities for subscribing a RemoteStream.
     */
    public static class AudioSubscriptionCapabilities {
        /**
         * List of AudioCodecParameters supported to subscribe a RemoteStream.
         */
        public final List<AudioCodecParameters> audioCodecs;

        AudioSubscriptionCapabilities(JSONObject audioObj) throws JSONException {
            audioCodecs = new ArrayList<>();

            JSONObject format = JsonUtils.getObj(audioObj, "format");
            audioCodecs.add(new AudioCodecParameters(AudioCodec.get(
                    JsonUtils.getString(format, "codec", "")),
                    JsonUtils.getInt(format, "channelNum", 0),
                    JsonUtils.getInt(format, "sampleRate", 0)));

            JSONObject audioOpt = JsonUtils.getObj(audioObj, "optional");
            if (audioOpt != null && audioOpt.has("format")) {
                JSONArray audioFormats = audioOpt.getJSONArray("format");
                for (int i = 0; i < audioFormats.length(); i++) {
                    JSONObject codecObj = audioFormats.getJSONObject(i);
                    AudioCodec codec = AudioCodec.get(JsonUtils.getString(codecObj, "codec", ""));
                    int channelNum = JsonUtils.getInt(codecObj, "channelNum", 0);
                    int sampleRate = JsonUtils.getInt(codecObj, "sampleRate", 0);
                    audioCodecs.add(new AudioCodecParameters(codec, channelNum, sampleRate));
                }
            }
        }
    }

    /**
     * Video capabilities for subscribing a RemoteStream.
     */
    public static class VideoSubscriptionCapabilities {
        /**
         * List of VideoCodecParameters supported to subscribe a RemoteStream.
         */
        public final List<VideoCodecParameters> videoCodecs;
        /**
         * List of resolutions supported to subscribe a RemoteStream.
         */
        public final List<HashMap<String, Integer>> resolutions;
        /**
         * List of framerates supported to subscribe a RemoteStream.
         */
        public final List<Integer> frameRates;
        /**
         * List of the multipliers of bitrate supported to subscribe a RemoteStream.
         */
        public final List<Double> bitrateMultipliers;
        /**
         * List of the intervals of keyframe supported to subscribe a RemoteStream.
         */
        public final List<Integer> keyFrameIntervals;

        VideoSubscriptionCapabilities(JSONObject videoObj) throws JSONException {
            videoCodecs = new ArrayList<>();
            resolutions = new ArrayList<>();
            frameRates = new ArrayList<>();
            bitrateMultipliers = new ArrayList<>();
            keyFrameIntervals = new ArrayList<>();

            // videoObj:
            // {'format': formatObj,
            //  'parameters': parametersObj,
            //  'optional': optionalObj}

            // formatObj
            // {'codec': codec}
            JSONObject formatObj = JsonUtils.getObj(videoObj, "format");
            String codec = JsonUtils.getString(formatObj, "codec", "");
            videoCodecs.add(new VideoCodecParameters(VideoCodec.get(codec)));

            // parametersObj
            // {'resolution': {'width': width, 'height': height},
            //  'framerate': framerate,
            //  'bitrate': bitrate,
            //  'keyFrameInterval': kfi}
            JSONObject parametersObj = JsonUtils.getObj(videoObj, "parameters");
            if (parametersObj != null) {
                if (parametersObj.has("resolution")) {
                    JSONObject reslutionObj = JsonUtils.getObj(parametersObj, "resolution");
                    HashMap<String, Integer> resolutionItem = new HashMap<>();
                    resolutionItem.put("width", JsonUtils.getInt(reslutionObj, "width", 0));
                    resolutionItem.put("height", JsonUtils.getInt(reslutionObj, "height", 0));
                    resolutions.add(resolutionItem);
                }
                frameRates.add(JsonUtils.getInt(parametersObj, "framerate", 0));
                keyFrameIntervals.add(JsonUtils.getInt(parametersObj, "keyFrameInterval", 0));
            }

            // optionalObj:
            // {'format': [codecObj],
            //  'parameters': optParamObj}

            // codecObj:
            // {'codec': codec}
            JSONObject optionalObj = JsonUtils.getObj(videoObj, "optional");
            if (optionalObj != null && optionalObj.has("format")) {
                JSONArray videoFormats = optionalObj.getJSONArray("format");
                for (int i = 0; i < videoFormats.length(); i++) {
                    JSONObject codecObj = videoFormats.getJSONObject(i);
                    VideoCodec videoCodec = VideoCodec.get(JsonUtils.getString(codecObj, "codec", ""));
                    videoCodecs.add(new VideoCodecParameters(videoCodec));
                }
            }

            // optParamObj:
            // {'resolution': [{'width': width, 'height': height}]
            //  'framerate': [framerate]
            //  'bitrate': [bitrate]
            //  'keyFrameInterval': [kfi]}
            if (optionalObj != null && optionalObj.has("parameters")) {
                JSONObject optParamObj = JsonUtils.getObj(optionalObj, "parameters");
                JSONArray resolutionsArray = optParamObj.getJSONArray("resolution");
                for (int i = 0; i < resolutionsArray.length(); i++) {
                    JSONObject resolution = resolutionsArray.getJSONObject(i);
                    HashMap<String, Integer> res = new HashMap<>();
                    res.put("width", JsonUtils.getInt(resolution, "width", 0));
                    res.put("height", JsonUtils.getInt(resolution, "height", 0));
                    resolutions.add(res);
                }
                JSONArray frameRatesArray = optParamObj.getJSONArray("framerate");
                for (int i = 0; i < frameRatesArray.length(); i++) {
                    frameRates.add(frameRatesArray.getInt(i));
                }
                JSONArray bitratesArray = optParamObj.getJSONArray("bitrate");
                for (int i = 0; i < bitratesArray.length(); i++) {
                    String bitrateString = bitratesArray.getString(i).substring(1);
                    bitrateMultipliers.add(Double.parseDouble(bitrateString));
                }
                JSONArray keyFrameIntervalsArray = optParamObj.getJSONArray("keyFrameInterval");
                for (int i = 0; i < keyFrameIntervalsArray.length(); i++) {
                    keyFrameIntervals.add(keyFrameIntervalsArray.getInt(i));
                }
            }

        }
    }
}
