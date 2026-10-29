import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  sendNotification,
  type NotifyKind,
  type NotifyResult,
} from "@/lib/notify";

export function useSendNotification() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      kind,
      id,
    }: {
      kind: NotifyKind;
      id: string;
    }): Promise<NotifyResult> => sendNotification(kind, id),
    onSettled: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["notices"] }),
        queryClient.invalidateQueries({ queryKey: ["notice"] }),
        queryClient.invalidateQueries({ queryKey: ["events"] }),
      ]);
    },
  });
}

