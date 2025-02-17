package io.github.apace100.calio.data;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SinglePreparationResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.profiler.Profiler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/***
 * Like JsonDataLoader, but provides a list of elements with an identifier, each element being loaded by a different
 * data pack. This allows overriding and merging several data files into one, similar to how tags work. There is no
 * guarantee on the order of the resulting list, so make sure to include some kind of "priority" system.
 */
public abstract class MultiJsonDataLoader extends SinglePreparationResourceReloader<Map<Identifier, List<JsonElement>>> {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int FILE_SUFFIX_LENGTH = ".json".length();
    private final Gson gson;
    private final String dataType;

    public MultiJsonDataLoader(Gson gson, String dataType) {
        this.gson = gson;
        this.dataType = dataType;
    }

    protected Map<Identifier, List<JsonElement>> prepare(ResourceManager resourceManager, Profiler profiler) {
        Map<Identifier, List<JsonElement>> map = Maps.newHashMap();
        int i = this.dataType.length() + 1;
        Iterator<Map.Entry<Identifier, Resource>> var5 = resourceManager.findResources(this.dataType, (id) -> {
            return id.getPath().endsWith(".json");
        }).entrySet().iterator();
        Set<String> resourcesHandled = new HashSet<>();
        while(var5.hasNext()) {
            Identifier identifier = var5.next().getKey();
            String string = identifier.getPath();
            Identifier identifier2 = new Identifier(identifier.getNamespace(), string.substring(i, string.length() - FILE_SUFFIX_LENGTH));
            resourcesHandled.clear();
            resourceManager.getAllResources(identifier).forEach(resource -> {
                if(!resourcesHandled.contains(resource.getResourcePackName())) {
                    resourcesHandled.add(resource.getResourcePackName());
                    try {
                        Throwable var10 = null;
                        try {
                            InputStream inputStream = resource.getInputStream();
                            Throwable var12 = null;

                            try {
                                Reader reader = resource.getReader();
                                Throwable var14 = null;

                                try {
                                    JsonElement jsonElement = (JsonElement) JsonHelper.deserialize(this.gson, (Reader)reader, (Class)JsonElement.class);
                                    if (jsonElement != null) {
                                        if(map.containsKey(identifier2)) {
                                            map.get(identifier2).add(jsonElement);
                                        } else {
                                            List<JsonElement> elementList = new LinkedList<>();
                                            elementList.add(jsonElement);
                                            map.put(identifier2, elementList);
                                        }
                                    } else {
                                        LOGGER.error("Couldn't load data file {} from {} as it's null or empty", identifier2, identifier);
                                    }
                                } catch (Throwable var62) {
                                    var14 = var62;
                                    throw var62;
                                } finally {
                                    if (reader != null) {
                                        if (var14 != null) {
                                            try {
                                                reader.close();
                                            } catch (Throwable var61) {
                                                var14.addSuppressed(var61);
                                            }
                                        } else {
                                            reader.close();
                                        }
                                    }

                                }
                            } catch (Throwable var64) {
                                var12 = var64;
                                throw var64;
                            } finally {
                                if (inputStream != null) {
                                    if (var12 != null) {
                                        try {
                                            inputStream.close();
                                        } catch (Throwable var60) {
                                            var12.addSuppressed(var60);
                                        }
                                    } else {
                                        inputStream.close();
                                    }
                                }

                            }
                        } catch (Throwable var66) {
                            var10 = var66;
                            throw var66;
                        }
                    } catch (IllegalArgumentException | IOException | JsonParseException var68) {
                        LOGGER.error("Couldn't parse data file {} from {}", identifier2, identifier, var68);
                    }
                }
            });
        }

        return map;
    }
}
