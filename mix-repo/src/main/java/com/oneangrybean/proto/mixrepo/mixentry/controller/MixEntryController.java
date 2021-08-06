package com.oneangrybean.proto.mixrepo.mixentry.controller;

import java.util.Arrays;
import java.util.List;

import com.oneangrybean.proto.mixrepo.mixentry.entity.MixEntry;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class MixEntryController {
    
    private static final List<MixEntry> MIX_ENTRIES =
        Arrays.asList(genEntry(1,"One"),genEntry(3,"Three"),genEntry(5,"Five"));

    private static MixEntry genEntry(Integer id, String name) {
        final MixEntry mixEntry = new MixEntry();
        mixEntry.setId(id);
        mixEntry.setName(name);
        return mixEntry;
    }

    @GetMapping("/mix-entry")
    @ResponseBody
    public List<MixEntry> list() {
        return MIX_ENTRIES;
    }

    @GetMapping("/mix-entry/{mixEntryId}")
    @ResponseBody
    public MixEntry get(@PathVariable("mixEntryId") Integer mixEntryId) {
        final MixEntry mixEntry =
            MIX_ENTRIES.stream()
                        .filter(me -> me.getId().equals(mixEntryId))
                        .findFirst()
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return mixEntry;
    }

    @GetMapping("/mix-entry/name/{mixEntryName}")
    @ResponseBody
    public MixEntry getByName(@PathVariable("mixEntryName") String mixEntryName) {
        final MixEntry mixEntry =
            MIX_ENTRIES.stream()
                        .filter(me -> me.getName().equals(mixEntryName))
                        .findFirst()
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return mixEntry;
    }
}
