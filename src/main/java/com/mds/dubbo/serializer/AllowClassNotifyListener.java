package com.mds.dubbo.serializer;

import java.util.Set;

public interface AllowClassNotifyListener {

    SerializeCheckStatus DEFAULT_STATUS = SerializeCheckStatus.STRICT;

    void notifyPrefix(Set<String> allowedList, Set<String> disAllowedList);

    void notifyCheckStatus(SerializeCheckStatus status);

    void notifyCheckSerializable(boolean checkSerializable);

}
