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

        // knob cc nums
        knobs['vcf-highpass'] = 28;
        knobs['vcf-lowpass'] = 74;

        // assign responders
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
        var packetList = Array.newClear(3);
        var idx = 0;

        // patch bay inputs
        inputs = (
            0: \total,
            19: \pink,
            23: \white,
            29: \signalOut
        );

        // action types
        actions = ();
        actions.patch = ();
        actions.single = ();

        // storage for connect/disconnect callbacks
        actions.single.connect = ();
        actions.single.disconnect = ();
        actions.patch.connect = ();
        actions.patch.disconnect = ();

        MIDIdef.cc(\io, {|msg|
            packetList[idx] = msg;
            idx = idx + 1;
            // collect 3 consecutive cc messages
            if(idx == 3) {
                this.parse(packetList);
                idx = 0;
            }
        }, [ 99, 98, 6 ]); // event, addr1, addr2
    }

    onConnect {|parameter, func|
        actions.single.connect[parameter] = func;
    }

    onDisconnect {|parameter, func|
        actions.single.disconnect[parameter] = func;
    }

    onPatch {|addr1, addr2, connectFunc, disconnectFunc|
        if(connectFunc.isFunction) {
            actions.patch.connect[addr1] ?? {
                actions.patch.connect[addr1] = ();
            };

            actions.patch.connect[addr1][addr2] = connectFunc;
        };

        if(disconnectFunc.isFunction) {
            actions.patch.disconnect[addr1] ?? {
                actions.patch.disconnect[addr1] = ();
            };

            actions.patch.disconnect[addr1][addr2] = disconnectFunc;
        }
    }

    parse {|message|
        var from, to, eventType;
        var event, addr1, addr2;
        #event, addr1, addr2 = message;

        from = inputs[addr1];
        to = inputs[addr2];

        eventType = (event == 4).if(\connect, \disconnect);

        if(from == to) {
            // just one connection
            actions.single[eventType][from].value;
        } {
            // see if there is a patch callback
            actions.patch[eventType][from] !? {
                actions.patch[eventType][from][to].value;
            }
        };
    }
}
