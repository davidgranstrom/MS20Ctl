// ===========================================================================
// Title         : MS20Ctl
// Description   : Interface class for Korg MS-20 MIDI controller
// Version       : 0.1
// Copyright (c) : David Granström 2016
// ===========================================================================

MS20Ctl : MS20PatchBay {
    var ctls;

    *new {
        ^super.new.initMS20Ctl;
    }

    initMS20Ctl {
        var knobs;

        MIDIClient.init;
        MIDIIn.connectAll;
        // MIDIIn.findPort("MS-20 Controller", "IN");

        ctls = ();
        knobs = ();

        knobs['vco1-waveform'] = 77;
        knobs['vco1-pw_adjust'] = 14;
        knobs['vco1-scale'] = 15;
        knobs['vco2-waveform'] = 82;
        knobs['vco2-pitch'] = 19;
        knobs['vco2-scale'] = 22;
        knobs['vco-portamento'] = 5;
        knobs['vco-master_tune'] = 18;
        knobs['vco-freq_mod_mg/t.ext'] = 12;
        knobs['vco-freq_mod_eg1/ext'] = 93;
        knobs['vco_mixer-vco1_level'] = 20;
        knobs['vco_mixer-vco2_level'] = 21;
        knobs['vcf-highpass_cutoff'] = 28;
        knobs['vcf-highpass_peak'] = 29;
        knobs['vcf-highpass_cutoff_mod_MG/T.EXT'] = 29;

        knobs['vcf-lowpass'] = 74;

        this.assignCtls(knobs);
    }

    assignCtls {|knobs|
        knobs.keysValuesDo {|key, cc|
            var ctl = MS20Knob(key, cc);
            ctls.put(key, ctl);
        };
    }

    onChange {|param, func|
        ctls[param].onChange_(func);
    }
}

MS20Knob {
    var key, cc;

    *new {|key, cc|
        ^super.newCopyArgs(("ms20_" ++ key).asSymbol, cc);
    }

    onChange_ {|func|
        MIDIdef.cc(key, func, cc);
    }

    free {
        MIDIdef.cc(key).free;
    }
}

MS20PatchBay {
    var actions, inputs;
    var lastConnection;

    *new {
        ^super.new.initPatchBay;
    }

    initPatchBay {
        var packetList = Array.new(3);

        // patch bay inputs
        inputs = (
            0: \total,
            19: \pink,
            23: \white,
            29: \signalOut
        );

        // storage for connect/disconnect callbacks
        actions = ();
        actions[\connect] = ();
        actions[\disconnect] = ();
        actions[\patchConnect] = ();
        actions[\patchDisconnect] = ();

        MIDIdef.cc(\io, {|msg|
            packetList = packetList.add(msg);
            // collect 3 consecutive cc messages
            if(packetList[2].notNil) {
                this.parse(packetList);
                packetList = Array.new(3);
            }
        }, [ 99, 98, 6 ]); // event, addr1, addr2
    }

    onConnect {|parameter, func|
        actions.connect[parameter] = func;
    }

    onDisconnect {|parameter, func|
        actions.disconnect[parameter] = func;
    }

    onPatch {|addr1, addr2, connectFunc, disconnectFunc|
        actions[\patchConnect][addr1] ?? {
            actions[\patchConnect][addr1] = ();
        };
        actions[\patchConnect][addr1][addr2] = connectFunc;

        // optional
        if(disconnectFunc.isFunction) {
            actions[\patchDisconnect][addr1] ?? {
                actions[\patchDisconnect][addr1] = ();
            };
            actions[\patchDisconnect][addr1][addr2] = disconnectFunc;
        }
    }

    parse {|message|
        var from, to;
        var event, addr1, addr2;
        #event, addr1, addr2 = message;

        from = inputs[addr1];
        to = inputs[addr2];

        // message.debug('message');

        switch (event)
        { 4 } {
            if(from == to) { // just one connection
                actions[\connect][from].value;
            } {
                // patch callback
                actions[\patchConnect][from][to].value;
            }
        }
        { 5 } {
            if(from == to) { // just one connection
                actions[\disconnect][from].value;
            } {
                // patch callback
                actions[\patchDisconnect][from][to].value;
            }
        };
    }
}
