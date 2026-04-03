import zmq
import json

from create_cot import CotMessage
from create_proto import CotProtoMessage

def read_mission_thread_zmq(pull_port: int, push_port: int, proto: bool = True):
    """
    Single shared ZMQ PULL → PUSH worker that handles both XML and proto.
    """
    import zmq
    from create_cot import CotMessage
    from create_proto import CotProtoMessage

    context = zmq.Context()
    pull = context.socket(zmq.PULL)
    push = context.socket(zmq.PUSH)

    pull.connect(f"tcp://localhost:{pull_port}")
    push.connect(f"tcp://localhost:{push_port}")

    Parser = CotProtoMessage if proto else CotMessage

    try:
        while True:
            data = pull.recv()
            try:
                msg = Parser(msg=data)

                if msg.is_sa():
                    continue

                # Mission change
                action, payload = msg.mission_change()
                if action:
                    push.send_json(
                        {'req_type': action, 'req_data': payload},
                        flags=zmq.NOBLOCK
                    )
                    continue

                # Fileshare
                f_hash, f_name = msg.fileshare()
                if f_hash:
                    push.send_json(
                        {'req_type': 'download_file', 'req_data': (f_hash, f_name)},
                        flags=zmq.NOBLOCK
                    )

            except Exception as e:
                print(f"read_mission_thread_zmq parse error: {type(e).__name__} -> {e.args}")

    except zmq.ContextTerminated:
        pass
    finally:
        pull.close()
        push.close()
